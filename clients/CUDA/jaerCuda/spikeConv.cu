// includes, system
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <math.h>
#include <assert.h>
#include <time.h>

// includes, project
#include <cutil.h>

#include <Winsock2.h>
#include <Ws2tcpip.h>

#include <windows.h>

#include "config.h"

// includes, kernels
#include "spikeConv_kernel.cu"

bool runCuda=1, tmpRunCuda=1; // tmp is for command processing

//===========================================================
// Server and client related code using Windows Socket
//===========================================================

extern char recvBuf[RECV_SOCK_BUFLEN];

//=========================================================
// Functions for jAER communication
//=========================================================

extern "C" {
	int jaerInit();
	int jaerRecv(); // fills up recvBuf with some spike data
	void jaerSendEvent(unsigned int addrx, unsigned int addry, unsigned long timeStamp, unsigned char type);
}

//==========================================================
// Functions that interface with golden reference
//==========================================================

extern "C" {
	int   templateConvInit(int selectType=TEMP_METHOD1, int templateType=TEMPLATE_DoG);
	void  computeGold(int numInSpike);
	int   extractJaerRawData( unsigned int* addr, unsigned long* timeStamp, char* Data, unsigned int len);	
	void  setInitLastTimeStamp(unsigned long timeStamp);
}

//===========================================================
// Functions related to dumping trace info and other matlab scripts
//===========================================================
extern "C"{
	void showMembranePotential(unsigned int* spikeAddr=NULL, int spikeCnt=0);
	void printResults(FILE* fpLog);
	void dumpTemplate(FILE* fp, char* fstr);
}



float inh_mem_potential[MAX_NUM_TEMPLATE];			// value of the inhibition potential
unsigned int  filteredSpike_addr[RECV_SOCK_BUFLEN/EVENT_LEN];	// array of filterred spike's address
unsigned long filteredSpike_timeStamp[RECV_SOCK_BUFLEN/EVENT_LEN];	// array of filtered spikes's timestamp

FILE *fpLog;						// Pointer to the log file...
long tot_fired_MO[MAX_NUM_TEMPLATE];  // total number of firing per neuronArray
int  inhFireCnt=0;					// total firing by inhibitory neuron
int   callCount=0;				// keeps track of number of times kernel is called
long  tot_filteredSpikes = 0;	// total number of filtered spikes since the start...
float accTimer = 0;				// total executing time is kept here...

int  num_object=MAX_NUM_TEMPLATE; 
int  radius_loc_inh = RADIUS_LOC_INH; // define the range of local inhibition project from one population to other populations
int template_type = TEMPLATE_Gab;
int template_method = TEMP_METHOD1;  
// delta_time is time in us that spikes are chunked together to be sent with common timestamp. 
// increasing speeds up processing but quantizes time more.
unsigned int delta_time=DELTA_TIME;

int debugLevel=DEBUG_LEVEL;

void* numFiring0AddrMO;		//  points to device memory. contains list of fired neurons for odd runs
void* numFiring1AddrMO;		//	points to device memory. contains list of fired neurons for even runs
void* devPtrSpikeAddr;		//  points to device memory. cpu copies spikes to gpu through this memory
void* devPtrSpikeTime;		//  points to device memory. cpu copies spikes time to this memory in gpu

unsigned int firedNeuron_addr[MAX_SENDING_SPIKES];

// initial values for the parameters are set here...
globalNeuronParams_t hostNeuronParams={
	MEMBRANE_THRESHOLD,
	MEMBRANE_TAU,
	MEMBRANE_POTENTIAL_MIN,
	MIN_FIRING_TIME_DIFF,
	EI_SYN_WEIGHT,
	IE_SYN_WEIGHT
};

bool sendGlobalNeuronParamsEnabled=1; // flag set from control thread
bool sendTemplateEnabled=1; // set from control thread
bool stopEnabled=0;  // set by jaer command 

cudaArray* cuArray; // used for texture template memory



/**********************************************************************************************************************/
/*****************************************  INITIATION  ***************************************************************/
/**********************************************************************************************************************/

/** Initialize CUDA device. If we have multiple CUDA device we are using device 0.
**/
void initiateDevice(int argc, char** argv)
{
	
	int dev;
	CUT_DEVICE_INIT(argc, argv);
	CUDA_SAFE_CALL(cudaSetDevice(0));
	CUDA_SAFE_CALL(cudaGetDevice(&dev));
	cudaDeviceProp deviceProp;                              
	CUDA_SAFE_CALL_NO_SYNC(cudaGetDeviceProperties(&deviceProp, dev)); 
	fprintf(stderr, "Using device %d: %s\n", dev, deviceProp.name); 
}


/** This function is to allocate device memory for the template matrix
	Also initiate the global neuron parameter structure
**/
void allocateDeviceMemory()
{	
	// allocate GPU cudaArray for convolution kernel, will be bound to a texture
	// cudaArray is 1d array of size sizeof(conv_template) which is M*n*n where M is # of templates and n is kernel size
	// cudaArray contains float values
	cudaChannelFormatDesc channelDesc = cudaCreateChannelDesc(32, 0, 0, 0, cudaChannelFormatKindFloat);
	CUDA_SAFE_CALL (cudaMallocArray (&cuArray, &channelDesc, sizeof(conv_template), 1));
	template_tex.normalized=0; // use int lookup for texture
	template_tex.filterMode=cudaFilterModePoint; // nearest texture value
	CUDA_SAFE_CALL (cudaBindTextureToArray (template_tex, cuArray));

	// get device addresses for output spikes related variables
	CUDA_SAFE_CALL (cudaGetSymbolAddress ( &numFiring0AddrMO, "numFiring0"));
	CUDA_SAFE_CALL (cudaGetSymbolAddress ( &numFiring1AddrMO, "numFiring1"));
	CUDA_SAFE_CALL (cudaGetSymbolAddress ( &devPtrSpikeAddr, "gpu_spikeAddr"));
	CUDA_SAFE_CALL (cudaGetSymbolAddress ( &devPtrSpikeTime, "gpu_spikeTime"));
	
	// already initialized statically but we do it again here to be sure to get the fields
	hostNeuronParams.threshold=MEMBRANE_THRESHOLD;
	hostNeuronParams.membraneTau=MEMBRANE_TAU;
	hostNeuronParams.membranePotentialMin=MEMBRANE_POTENTIAL_MIN;
	hostNeuronParams.minFiringTimeDiff=MIN_FIRING_TIME_DIFF;
	hostNeuronParams.eISynWeight=EI_SYN_WEIGHT;
	hostNeuronParams.iESynWeight=IE_SYN_WEIGHT;
	
	// bounds checking for the number of objects
	if(template_type != TEMPLATE_Gab){
		if(num_object>MAX_NUM_OBJECT) num_object=MAX_NUM_OBJECT ;
	}else{
		if(num_object>GABOR_MAX_NUM_ORIENTATION) num_object=GABOR_MAX_NUM_ORIENTATION ;
	}
}


/** This function is to initialize the variables used in neural network computation
**/
void initializeNeurons()
{
	void *devPtr;
	
	// initiate the elements of the device membrane potential array
	cudaThreadSynchronize();
	CUDA_SAFE_CALL ( cudaGetSymbolAddress(&devPtr, "gpu_membranePotential"));
#pragma warning(disable:4313)
	printf("Zeroing membrane Potentials on GPU (loc = %x, size = %d bytes)\n", devPtr, sizeof(gpu_membranePotential));
#pragma warning(default:4313)
	CUDA_SAFE_CALL( cudaMemset( devPtr, 0, sizeof(membranePotential)));
	CUT_CHECK_ERROR("after initialize gpu_membranePotential");
	cudaThreadSynchronize();
	
	// initiate the elements of the device counter of firing for each neuron in the current cycle
	cudaThreadSynchronize();
	CUDA_SAFE_CALL ( cudaGetSymbolAddress(&devPtr, "gpu_curNumFiring"));
#pragma warning(disable:4313)
	printf("Zeroing membrane Potentials on GPU (loc = %x, size = %d bytes)\n", devPtr, sizeof(gpu_curNumFiring));
#pragma warning(default:4313)
	CUDA_SAFE_CALL( cudaMemset( devPtr, 0, sizeof(membranePotential)));
	CUT_CHECK_ERROR("after initialize gpu_curNumFiring");
	cudaThreadSynchronize();

	// initiate the elements of the device last time stamp array 
	CUDA_SAFE_CALL(cudaGetSymbolAddress(&devPtr, "gpu_lastTimeStamp"));
#pragma warning(disable:4313)
	printf("Copying last time stamp value to GPU (loc = %x)\n", devPtr);
#pragma warning(default:4313)
	CUDA_SAFE_CALL( cudaMemcpy( devPtr, &lastTimeStamp[0][0], sizeof(lastTimeStamp), cudaMemcpyHostToDevice));
	cudaThreadSynchronize();
	
	printf("Initializing spike counter\n");
	CUDA_SAFE_CALL( cudaMemset (numFiring0AddrMO, 0, sizeof(int)*MAX_NUM_TEMPLATE));		
	CUDA_SAFE_CALL( cudaMemset (numFiring1AddrMO, 0, sizeof(int)*MAX_NUM_TEMPLATE));	
	CUT_CHECK_ERROR("after initialize gpu_lastTimeStamp");	
	cudaThreadSynchronize();
		
	memset(inh_mem_potential,0,sizeof(float)*MAX_NUM_TEMPLATE);
	memset(tot_fired_MO,0,sizeof(long)*MAX_NUM_TEMPLATE);
	
	fflush(stdout);
}

/**********************************************************************************************************************/
/*****************************************  CLEAN UP  *****************************************************************/
/**********************************************************************************************************************/

/** This function cleans up the allocated device memory
**/
void cudaClean()
{
	CUDA_SAFE_CALL(cudaUnbindTexture(template_tex));
	CUDA_SAFE_CALL (cudaFreeArray (cuArray));
}

/**********************************************************************************************************************/
/*****************************************  PARAMETER CHANGE  *********************************************************/
/**********************************************************************************************************************/

/** This function is to online check if the parameters has been changed **/
void onlineParamChange()
{
	/** check if tmpRunCuda has been changed **/
	if(tmpRunCuda!=runCuda){
		if(tmpRunCuda){
			sendGlobalNeuronParamsEnabled=true;
			sendTemplateEnabled=true;
			runCuda=1;
		}else{
			sendGlobalNeuronParamsEnabled=false;
			sendTemplateEnabled=false;
			runCuda=0;
		}
	} // make sure we get parameters to cuda and only change runCuda here in loop

	/** check if neuron parameters has been changed **/
	if(sendGlobalNeuronParamsEnabled) {
		sendGlobalNeuronParamsEnabled=0;
		
		cudaThreadSynchronize();
		fprintf(stdout, "Copying neuron global constants to device\n");
		CUDA_SAFE_CALL(cudaMemcpyToSymbol("constNeuronParams",&hostNeuronParams,sizeof(globalNeuronParams_t),(size_t)0,cudaMemcpyHostToDevice));
		CUT_CHECK_ERROR("Copy neuron global constants to device");
		
		fprintf(stderr,"Params th=%f, tau=%f, pot=%f, time=%f, eIWt=%f, iEWt=%f\n", 
			hostNeuronParams.threshold, hostNeuronParams.membraneTau, hostNeuronParams.membranePotentialMin, hostNeuronParams.minFiringTimeDiff,
			hostNeuronParams.eISynWeight, hostNeuronParams.iESynWeight);
			
		CUDA_SAFE_CALL(cudaMemcpyToSymbol("const_radius_loc_inh",&radius_loc_inh,sizeof(int),(size_t)0,cudaMemcpyHostToDevice));
		CUT_CHECK_ERROR("Copy radius_loc_inh");
		fprintf(stderr,"Param radius_loc_inh=%d,", radius_loc_inh);
		
		int size_loc_inh = radius_loc_inh*2+1;
		CUDA_SAFE_CALL(cudaMemcpyToSymbol("const_size_loc_inh",&size_loc_inh,sizeof(int),(size_t)0,cudaMemcpyHostToDevice));
		CUT_CHECK_ERROR("Copy size_loc_inh");
		fprintf(stderr," size_loc_inh=%d\n", size_loc_inh);
			
	}
	
	/** check if template should be send to device **/
	if(sendTemplateEnabled){
		sendTemplateEnabled=0;
		
		// generate template only when the template type is not Gabor function, otherwise, it is generated from JEAR 
		if(template_type != TEMPLATE_Gab)
			templateConvInit(template_method,template_type);
		
		// send template to GPU 
		cudaThreadSynchronize();
	#pragma warning(disable:4313)
		printf("Copying templates to GPU (loc = %x, size = %d bytes)\n", cuArray, sizeof(conv_template));
	#pragma warning(default:4313)
		// copy the template to cuda texture array
		CUDA_SAFE_CALL( cudaMemcpyToArray(cuArray, 0, 0, conv_template, sizeof(conv_template), cudaMemcpyHostToDevice) );
		CUT_CHECK_ERROR("after send Template");
				
		CUDA_SAFE_CALL( cudaMemcpyToSymbol("const_num_object",&num_object,sizeof(int),(size_t)0,cudaMemcpyHostToDevice));
		CUT_CHECK_ERROR("after send num_object"); 
			
		cudaThreadSynchronize();
	}
}

/**********************************************************************************************************************/
/*****************************************  INPUT RECEIVING AND FILTERING  ********************************************/
/**********************************************************************************************************************/

/** This function is to receive events from jaer, and filter them through an refractory filter
 *  return:		-1			error occurs when REPLAY_MODE is not defined
				-2			error occurs when REPLAY_MODE is defined
				numSpikes	otherwise
 **/
int recvFilterSpikes()
{
	int iResult;
	int numEvents;
	int numSpikes;
	 
	#ifndef REPLAY_MODE
			iResult=jaerRecv(); // in recvBuf, returns immediately if input port not yet open, blocks if waiting socket open
			//jaerServerSend(recvBuf, iResult); // debug to echo back data to jaer
			if ( iResult > 0 ) {			
				numEvents=iResult/EVENT_LEN;
				if(debugLevel>0) {
					printf("Unfiltered events received: %d\n", numEvents);
					fflush(stdout);
				}
			// don't quit if we receive 0 events (tobi), just continue
			} else if ( iResult == 0 ){
				Sleep(1);
				//fprintf(stderr, "Recieved packet with 0 events, continuing loop\n");		
				return -1;
			} else {
				fprintf(stderr, "recv failed: WSAGetLastError=%d\n", WSAGetLastError());
				fflush(stderr);
				Sleep(1);
				return -1;
			}
	#else
			numEvents = 0;
			iResult = 0;
	#endif

			
			numSpikes = extractJaerRawData(filteredSpike_addr, filteredSpike_timeStamp, recvBuf, numEvents);		
	#ifdef REPLAY_MODE
			if (numSpikes == -1) {
				fprintf(stderr,"readNNFilter returned -1 (error), continuing\n");
				return -2;
			}
	#else
			if (numSpikes == -1) {
				fprintf(stderr,"readNNFilter returned -1 (error), continuing\n");
				fflush(stderr);
				return -1;
			}
	#endif
	
	tot_filteredSpikes += numSpikes;
	if(debugLevel>0) printf("number of spikes after refractory filter = %d\n", numSpikes);
	
	return numSpikes;
}

/**********************************************************************************************************************/
/*****************************************  OUTPUT SENDING  ***********************************************************/
/**********************************************************************************************************************/

/** This function is to send the output spikes from E and I neurons back to jaer
 *  @param:		timeStamp		current time stamp		
 *  @param:		nfiredMO		the array records the number of excitatory spikes generated within each population
 *  @param:		n_iNeuronFired	number of inhibitory spikes generated within the current cycle 
 
 **/
void cudaCopySpikesFromGPU2jAER(unsigned long timeStamp, int* nfiredMO, int n_iNeuronFired)
{
	int net_firing = 0;
	// send the output spikes to jaer
#ifndef REPLAY_MODE
		
	// copy fired neurons	
	for ( int i=0; i < num_object; i++) {

		if(nfiredMO[i] >= MAX_FIRING) {
			if(debugLevel>-1) printf("# output spikes in kernel(%d) overflowed kernel buffer, dropping excess\n",nfiredMO[i]);
			continue;
			//TODO: copy them in smaller chunks and send to the jAER
		}

		// copy the fired array from GPU to CPU
		if (nfiredMO[i]){
			CUDA_SAFE_CALL(cudaMemcpyFromSymbol(&firedNeuron_addr[0], "firedNeuronAddr", sizeof(int)*nfiredMO[i], sizeof(int)*MAX_FIRING*i, cudaMemcpyDeviceToHost));
			net_firing = net_firing + nfiredMO[i];
		}

		// TODO cast from raw address back to addrx, addry, shouldn't be necessary
		for(int j = 0; j < nfiredMO[i]; j++) {
			unsigned int addrx = (firedNeuron_addr[j]) & 0x7f;
			unsigned int addry = (firedNeuron_addr[j]>>8)&0x7f;
		
			// accumulate fired neuron and send to jaer
			jaerSendEvent(addrx,addry,timeStamp,i);
		}
	}

	if(debugLevel > 0)
		fprintf(stdout,"cudaCopySpikesFromGPU2jAER: sent %d excitatory spikes to jaer\n", net_firing);
		
	// send one spike to jaer if any of the inhibitory neuron fires
	if(n_iNeuronFired){
		for(int i=0; i < n_iNeuronFired; i++){
			jaerSendEvent(1,1,timeStamp,0); 
		} 
		if(debugLevel > 0)
			fprintf(stdout,"cudaCopySpikesFromGPU2jAER: sent %d inhibitory spike to jaer\n", n_iNeuronFired);
	}		
		
#else
	// ??? TODO: we bring all spike info into one firedNeuron_addr array. So all the
	// object firing is lumped into one AER display. Use different
	// color to distinguish output from different object
	int accAddr=0;
	for ( int i=0; i < num_object; i++) {

		if(accAddr >= MAX_SENDING_SPIKES) {
			printf("Total generated spikes is more than sending array size;\n");
			accAddr = 0;
		}

		accAddr += nfiredMO[i];
	}			
	//assert(accAddr == net_firing);
#endif		
}

/**********************************************************************************************************************/
/*****************************************  UPDATE INEURON  ***********************************************************/
/**********************************************************************************************************************/

/** function that updates the membrane potential of inibitory WTA neurons. Inhibitory neuron for each object is updated separately
 * The state of firing is described by a boolean value per object
 * TODO: Make the inhibitory neurons also leaky. Currently the inhibitory neurons are NOT leaky...
 * @param:	numFiringAddr	the device memory address recording the number of firing per population
 * @param:  nfiredMO		array records the number of spikes generated within this cycle
 * @param:	iNeuronFired	each bit records if the global inhibitory neuron of the corresponding excitatory population has fired during the current cycle
 * returns: n_fired_Mo_Inh:	the number of inhibitory neurons fired during the current cycle
 **/
int cudaUpdateINeuron(void* numFiringAddr, int* nfiredMO, char* iNeuronFired)
{
		int n_fired_Mo_Inh = 0;	// the total number of inhibitory spikes generated within one cycle
				
		// copy the number of template layer neurons that have fired...
		CUDA_SAFE_CALL(cudaMemcpy(nfiredMO, numFiringAddr, sizeof(int)*num_object, cudaMemcpyDeviceToHost));
		CUT_CHECK_ERROR("After copying number of generated spikes from GPU to CPU");
		
		if(debugLevel>1){
			printf("# spikes fired by object layers: ");
			for(int i=0;i<num_object;i++){
				printf("%d, ",nfiredMO[i]);
			}
			printf("\n");
		}
	
		//update inhibitory neuron membrane potentials
		for ( int i=0; i < num_object; i++) {
			tot_fired_MO[i] += nfiredMO[i]; //per object firing
			
			inh_mem_potential[i] = inh_mem_potential[i] + hostNeuronParams.eISynWeight*nfiredMO[i];
			if (inh_mem_potential[i] > hostNeuronParams.threshold) {
				inh_mem_potential[i] = 0.0;
				inhFireCnt++;	
				n_fired_Mo_Inh++;
				*iNeuronFired = (char)(*iNeuronFired | (0x01 << i));  //set the corresponding bit to 1 if inhibitory neuron fires
			}				
		}
		
		return n_fired_Mo_Inh;
}

/** function that updates the membrane potential of the global inibitory WTA neurons. Global inhibitory neuron is updated with the number of fired spikes depending on 
 * the total number of E spikes generated within the cycle
 * TODO: Make the inhibitory neurons also leaky. Currently the inhibitory neurons are NOT leaky...
 * @param:	numFiringAddr	the device memory address recording the number of firing per population
 * @param:  nfiredMO		array records the number of spikes generated within this cycle
 * returns: n_fired_Mo_Inh:		the number of spikes the global inhibitory neuron fired during the current cycle
 **/
int cudaUpdateINeuronMultiSp(void* numFiringAddr, int* nfiredMO)
{
		int n_fired_Mo_Inh = 0; // a byte record which inhibitory neuron has fired
				
		// copy the number of template layer neurons that have fired...
		CUDA_SAFE_CALL(cudaMemcpy(nfiredMO, numFiringAddr, sizeof(int)*num_object, cudaMemcpyDeviceToHost));
		CUT_CHECK_ERROR("After copying number of generated spikes from GPU to CPU");
		
		if(debugLevel>1){
			printf("# spikes fired by object layers: ");
			for(int i=0;i<num_object;i++){
				printf("%d, ",nfiredMO[i]);
			}
			printf("\n");
		}
	
		int firingWithinCycle = 0;
		//update inhibitory neuron membrane potentials
		for ( int i=0; i < num_object; i++) {
			tot_fired_MO[i] += nfiredMO[i]; //per object firing
			firingWithinCycle += nfiredMO[i]; 
		}
			
		// update the global inhibitory membrane potential and calculate the number of spikes fired
		inh_mem_potential[0] = inh_mem_potential[0] + hostNeuronParams.eISynWeight*firingWithinCycle;
		if (inh_mem_potential[0] > hostNeuronParams.threshold) {
			n_fired_Mo_Inh = (int)floor(inh_mem_potential[0]/hostNeuronParams.threshold);
			inhFireCnt += n_fired_Mo_Inh;				
			inh_mem_potential[0] = 0.0;
		}
		
		return n_fired_Mo_Inh;
}



/**********************************************************************************************************************/
/*****************************************  GPU COMPUTING  ************************************************************/
/**********************************************************************************************************************/

// keep tracks of how many spikes that are suppied to GPU kernel
// For performance evaluation and analysis....
int paramLenArr[PARAM_LEN_SIZE];

/** GPU computation on the WTA neural network
 *  @param:		gridExcDim		grid dimension for updating excitatory membrane potentials
 *  @param:		threadExcDim	thread dimension for updating excitatory membrane potentials
 *  @param:		gridInhDim		grid dimension for updating excitatory membrane potentials after the firing of global inhibitory neuron
 *  @param:     threadInhDim	thread dimension for updating excitatory membrane potentials after the firing of global inhibitory neuron
 *	@param:		firingId		the address of firingId, which toggles between 0/1, so that one is reset (done in the kernel) and the other is used to count the number of spikes generated during one cycle 
 *  @param:		numInSpikes		the number of input spikes after refractory filtering
 **/
void GPU_MODE(dim3 gridExcDim, dim3 threadExcDim, dim3 gridInhDim, dim3 threadInhDim, int* firingId, int numInSpikes){
	
	// initiate variables with the first spike
	int  index_start=0;
	int spikeLen = 0;
	unsigned long spikeTimeStampV = filteredSpike_timeStamp[0]; // set the global timestamp for packet	
	int cpu_nfiredMO[MAX_NUM_TEMPLATE];	// number of neurons that got fired in the last kernel call
	
	// this loop iterates over spikes in the packet, calling the kernel periodically when it has collected enough
	// spikes. after copying the spike addresses to GPU memory, it passes struct params to the kernel. 
	// then it reads the number of neurons that fired and copies back the 
	// fired neuron addresses.
	for (int spk_i = 0; spk_i < numInSpikes; spk_i++ ) {
	
		/*********************************************************/
		/****Generate input event packet and send to GPU**********/
		/*********************************************************/
		
		if (spk_i==(numInSpikes-1)){
			// this is the last spike then just go and process the bufferred spikes in the GPU.
			spikeLen++; // just increment number of spikes to be processed
		}
		else if (spikeLen == (GPU_MAX_SPIKE_PACKETS)) {
			// our buffer is full. so go and process existing spike buffer the current spike will be part of next group..
		}
		else if ((filteredSpike_timeStamp[spk_i] - spikeTimeStampV) < delta_time) {		
			// if we're not the first or last spike or at the limit, and
			// If the current time stamp of a spike is within the delta_time then
			// we buffer the spike and start reading the next spike...
			spikeLen++;
			continue;
		}				

		// Keep track of the number of spikes that are buffered and sent to CUDA. 
		// This is useful to understand the performance, as more grouping 
		// means good performance...and CUDA kernel launch overhead is reduced.
 		if(callCount < PARAM_LEN_SIZE)
			paramLenArr[callCount]=spikeLen;						
		if(debugLevel>1){
			printf("copying %d spike addresses to GPU\n",spikeLen);
		}

		if(spikeLen != 0){
			// copy spikes addresses to GPU
			CUDA_SAFE_CALL(cudaMemcpy( devPtrSpikeAddr, &filteredSpike_addr[index_start], sizeof(int)*spikeLen, cudaMemcpyHostToDevice));
			CUT_CHECK_ERROR("Copy spike addresses to GPU");
			CUDA_SAFE_CALL(cudaMemcpy( devPtrSpikeTime, &filteredSpike_timeStamp[index_start], sizeof(unsigned long)*spikeLen, cudaMemcpyHostToDevice));
			CUT_CHECK_ERROR("Copy spike timestamps to GPU");

	#if !MEASUREMENT_MODE			
			fprintf( fpLog, "%d => len=%d t=%d a=%d\n", callCount, spikeLen, spikeTimeStampV, filteredSpike_addr[index_start]);
	#endif

			/*********************************************************/
			/*******Call multi-spike kernel***************************/
			/*********************************************************/
			
			// firingId is a toggle 0/1 that is used for odd/even kernel launches.
			// the kernel writes the number of fired neurons for each template in the array
			// pointed to by numFiringArrayAddr, at the same time, it also sets the array pointed to 
			// by resetFiringArrayAddr all to zero. The host uses the numFiring values to update the WTA neurons.
			int* numFiringArrayAddr   = (int*)((*firingId)?numFiring0AddrMO:numFiring1AddrMO);
			int* resetFiringArrayAddr = (int*)((*firingId)?numFiring1AddrMO:numFiring0AddrMO); // TODO, this array is unused now
			
			if(debugLevel>1){
				printf("calling multi object convNN_multiSpikeKernel with gridDim=(%d,%d,%d), threadDim=(%d,%d,%d)\n",gridExcDim.x, gridExcDim.y, gridExcDim.z, threadExcDim.x, threadExcDim.y,threadExcDim.z);
			}
			
			CUT_CHECK_ERROR("convNN_multiSpikeKernel Before kernel execution");
			convNN_multiSpike_Kernel <<< gridExcDim, threadExcDim >>> (spikeLen, numFiringArrayAddr, resetFiringArrayAddr);
			CUT_CHECK_ERROR("convNN_multiSpikeKernel Kernel execution failed");	
			cudaThreadSynchronize();
				
			if(debugLevel>1) fprintf(stderr, "Kernel executed %d times...\n", callCount);
				
		#if RECORD_MEMBRANE_POTENTIAL
			showMembranePotential(&filteredSpike_addr[index_start],spikeLen); // only for debug
		#endif
			
			/***********************************************************************************/
			/********Update membrane potential of inhibitory neurons and call WTA kernel********/
			/***********************************************************************************/ 
			
			// execute updation of iNeuron potential in CPU
			// the single WTA neuron gets excited by the total number of spikes from the convolution
		#ifndef GLOBAL_INH    //local inhibition
			char iNeuronFired = 0;
			int n_iNeuronFired = cudaUpdateINeuron(numFiringArrayAddr, cpu_nfiredMO, &iNeuronFired);
			
			if (n_iNeuronFired) {
				
				if(debugLevel>1){
				printf("calling winner take all kernel WTAKernelMO with gridDim=(%d,%d,%d), threadDim=(%d,%d,%d)\n",gridInhDim.x, gridInhDim.y, gridInhDim.z, threadInhDim.x, threadInhDim.y,threadInhDim.z);
				}
				
				// execute iNeuronCalculations; inhibition of all other neurons in GPU
				CUT_CHECK_ERROR("WTAKernelMO Before kernel execution");
				WTAKernelMO <<< gridInhDim, threadInhDim >>> (numFiringArrayAddr, iNeuronFired);
				CUT_CHECK_ERROR("WTAKernelMO After kernel execution");
				cudaThreadSynchronize();
			}
		#else		// global inhibition
			int n_iNeuronFired = cudaUpdateINeuronMultiSp(numFiringArrayAddr, cpu_nfiredMO);
			
			if (n_iNeuronFired) {
				
				if(debugLevel>1){
				printf("calling winner take all kernel WTAKernelMOGlob with gridDim=(%d,%d,%d), threadDim=(%d,%d,%d)\n",gridInhDim.x, gridInhDim.y, gridInhDim.z, threadInhDim.x, threadInhDim.y,threadInhDim.z);
				}
				
				// execute iNeuronCalculations; inhibition of all other neurons in GPU
				CUT_CHECK_ERROR("WTAKernelMOGlob Before kernel execution");
				WTAKernelMOGlob <<< gridInhDim, threadInhDim >>> (numFiringArrayAddr, n_iNeuronFired);
				CUT_CHECK_ERROR("WTAKernelMOGlob After kernel execution");
				cudaThreadSynchronize();
			}
		#endif
			
			// toggle the id after each cycle to reset either numFiring0AddrMO or numFiring1AddrMO
			*firingId = (*firingId ) ? 0 : 1;
			
			/************************* send output spikes back to jaer  ******************/
			cudaCopySpikesFromGPU2jAER(spikeTimeStampV, cpu_nfiredMO, n_iNeuronFired);
			
			
			/************************* update counters ***********************************/
			callCount++;
			spikeTimeStampV = filteredSpike_timeStamp[spk_i]; // store the time stamp of spike for next grouping
			spikeLen  = 0;							  // reset length
			index_start = spk_i;					  // reset the index
		}
		
	} // iterate over spikes in this packet		
}


/** GPU computation on the local WTA neural network with inhibitory coupling between different features at the same network position
 *  @param:		gridExcDim		grid dimension for updating excitatory membrane potentials
 *  @param:		threadExcDim	thread dimension for updating excitatory membrane potentials
 *	@param:		firingId		the address of firingId, which toggles between 0/1, so that one is reset (done in the kernel) and the other is used to count the number of spikes generated during one cycle 
 *  @param:		numInSpikes		the number of input spikes after refractory filtering
 **/
void GPU_MODE_LOCAL_WTA(dim3 gridExcDim, dim3 threadExcDim, int* firingId, int numInSpikes){
	
	// initiate variables with the first spike
	int  index_start=0;
	int spikeLen = 0;
	unsigned long spikeTimeStampV = filteredSpike_timeStamp[0]; // set the global timestamp for packet	
	int cpu_nfiredMO[MAX_NUM_TEMPLATE];	// number of neurons that got fired in the last kernel call
	
	// this loop iterates over spikes in the packet, calling the kernel periodically when it has collected enough
	// spikes. after copying the spike addresses to GPU memory, it passes struct params to the kernel. 
	// then it reads the number of neurons that fired and copies back the 
	// fired neuron addresses.
	for (int spk_i = 0; spk_i < numInSpikes; spk_i++ ) {
	
		/*********************************************************/
		/****Generate input event packet and send to GPU**********/
		/*********************************************************/
		
		if (spk_i==(numInSpikes-1)){
			// this is the last spike then just go and process the bufferred spikes in the GPU.
			spikeLen++; // just increment number of spikes to be processed
		}
		else if (spikeLen == (GPU_MAX_SPIKE_PACKETS)) {
			// our buffer is full. so go and process existing spike buffer the current spike will be part of next group..
		}
		else if ((filteredSpike_timeStamp[spk_i] - spikeTimeStampV) < delta_time) {		
			// if we're not the first or last spike or at the limit, and
			// If the current time stamp of a spike is within the delta_time then
			// we buffer the spike and start reading the next spike...
			spikeLen++;
			continue;
		}				

		// Keep track of the number of spikes that are buffered and sent to CUDA. 
		// This is useful to understand the performance, as more grouping 
		// means good performance...and CUDA kernel launch overhead is reduced.
 		if(callCount < PARAM_LEN_SIZE)
			paramLenArr[callCount]=spikeLen;						
		if(debugLevel>1){
			printf("copying %d spike addresses to GPU\n",spikeLen);
		}
		
		if(spikeLen != 0){
			// copy spikes addresses to GPU
			CUDA_SAFE_CALL(cudaMemcpy( devPtrSpikeAddr, &filteredSpike_addr[index_start], sizeof(int)*spikeLen, cudaMemcpyHostToDevice));
			CUT_CHECK_ERROR("Copy spike addresses to GPU");
			CUDA_SAFE_CALL(cudaMemcpy( devPtrSpikeTime, &filteredSpike_timeStamp[index_start], sizeof(unsigned long)*spikeLen, cudaMemcpyHostToDevice));
			CUT_CHECK_ERROR("Copy spike timestamps to GPU");

	#if !MEASUREMENT_MODE			
			fprintf( fpLog, "%d => len=%d t=%d a=%d\n", callCount, spikeLen, spikeTimeStampV, filteredSpike_addr[index_start]);
	#endif

			/*********************************************************/
			/*******Call multi-spike kernel***************************/
			/*********************************************************/
			
			// firingId is a toggle 0/1 that is used for odd/even kernel launches.
			// the kernel writes the number of fired neurons for each template in the array
			// pointed to by numFiringArrayAddr, at the same time, it also sets the array pointed to 
			// by resetFiringArrayAddr all to zero. The host uses the numFiring values to update the WTA neurons.
			int* numFiringArrayAddr   = (int*)((*firingId)?numFiring0AddrMO:numFiring1AddrMO);
			int* resetFiringArrayAddr = (int*)((*firingId)?numFiring1AddrMO:numFiring0AddrMO); // TODO, this array is unused now
			
			if(debugLevel>1){
				printf("calling multi object convNN_LocalWTA_Kernel with gridDim=(%d,%d,%d), threadDim=(%d,%d,%d)\n",gridExcDim.x, gridExcDim.y, gridExcDim.z, threadExcDim.x, threadExcDim.y,threadExcDim.z);
			}
			
			CUT_CHECK_ERROR("convNN_multiSpikeKernel Before kernel execution");
			convNN_LocalWTA_Kernel1 <<< gridExcDim, threadExcDim >>> (spikeLen, numFiringArrayAddr, resetFiringArrayAddr);
			CUT_CHECK_ERROR("convNN_multiSpikeKernel Kernel execution failed");	
			cudaThreadSynchronize();
				
			if(debugLevel>1) fprintf(stderr, "Kernel executed %d times...\n", callCount);
				
		#if RECORD_MEMBRANE_POTENTIAL
			showMembranePotential(&filteredSpike_addr[index_start],spikeLen); // only for debug
		#endif
		
			// copy the number of neurons that have fired...
			CUDA_SAFE_CALL(cudaMemcpy(cpu_nfiredMO, numFiringArrayAddr, sizeof(int)*num_object, cudaMemcpyDeviceToHost));
			CUT_CHECK_ERROR("After copying number of generated spikes from GPU to CPU");
			
			if(debugLevel>1){
				printf("# spikes fired by object layers: ");
				for(int i=0;i<num_object;i++){
					printf("%d, ",cpu_nfiredMO[i]);
				}
				printf("\n");
			}
		
			//count total number of output spikes
			for ( int i=0; i < num_object; i++) {
				tot_fired_MO[i] += cpu_nfiredMO[i]; //per object firing
			}	
			
			// toggle the id after each cycle to reset either numFiring0AddrMO or numFiring1AddrMO
			*firingId = (*firingId ) ? 0 : 1;
			
			/************************* send output spikes back to jaer  ******************/
			cudaCopySpikesFromGPU2jAER(spikeTimeStampV, cpu_nfiredMO, 0);	

			/************************* update counters ***********************************/
			callCount++;
			spikeTimeStampV = filteredSpike_timeStamp[spk_i]; // store the time stamp of spike for next grouping
			spikeLen  = 0;							  // reset length
			index_start = spk_i;					  // reset the index
		}
		
	} // iterate over spikes in this packet		
}


/**********************************************************************************************************************/
/*****************************************  MAIN FUNCTION INTERACTING WITH JAER AND CUDA GPU  *************************/
/**********************************************************************************************************************/

int runjaerCUDA( int argc, char** argv)
{
	int setTimeStamp = 1;
	
	// this is switched alternative to 0 and 1. every time kernel is called
	int firingId = 1;
	
	// kernel evaluates multiple convolution kernels for global WTA scheme
	// setup parameters for multi-object case
	// dimension defined for GPU_MODE
	dim3 gridExcDim(8,8*num_object,1);
	dim3 threadExcDim(16,16,1);
	
	dim3 gridInhDim(128,num_object,1);
	dim3 threadInhDim(128,1,1);
	
	// dimension defined for GPU_MODE_LOCAL_WTA
	dim3 gridExcDim1(8,8,1);
	dim3 threadExcDim1(16,16,1);
	
	/** initiate jaer **/
#ifndef REPLAY_MODE
	jaerInit();
#endif

	/** initiate gpu device **/
	initiateDevice(argc, argv);

	/** allocates and notes down various memory in the GPU side...	 **/
	allocateDeviceMemory();
	
	// initiate timer
	unsigned int timer = 0;
	CUT_SAFE_CALL( cutCreateTimer( &timer));
	CUT_SAFE_CALL( cutStartTimer( timer));
	
	/** Receive data until the server closes the connection **/
	do { 
		
		/** main loop **/
		
		/** online check if the parameters has been changed **/
		onlineParamChange();
		
		/** Receive data from jaer **/
		int numSpikes = recvFilterSpikes();
		if(numSpikes == -1){
			continue;
		}else if(numSpikes == -2){
			break;
		} // if numSpikes is not smaller than 0, it implies that numSpikes > 0
		
		if(debugLevel>1){
			printf("*** start cycle\n");
		}
		
		/** initiate the variables involved in neural network computation **/
		if(setTimeStamp == 1)	{
			//first time we need to set the timeStamp value appropriately...
			setInitLastTimeStamp(*(filteredSpike_timeStamp));
			
			if(runCuda) {				
				initializeNeurons();
			}
			
			setTimeStamp = 0;
		}

		//////////////////////////////////////////
		//        CUDA-GPU MODEL				//
		//////////////////////////////////////////
		if(runCuda) {
		
		#if LOCAL_WTA
			GPU_MODE_LOCAL_WTA(gridExcDim1,threadExcDim1, &firingId, numSpikes);
		#else
			GPU_MODE(gridExcDim,threadExcDim, gridInhDim, threadInhDim, &firingId, numSpikes);
		#endif
			
		} // end if(runCuda)

		//////////////////////////////////////////
		//        CPU MODEL						//
		//////////////////////////////////////////		
		else {		
			// compute reference solution
			computeGold(numSpikes);
			
		#if RECORD_MEMBRANE_POTENTIAL
			showMembranePotential();
		#endif
		}

	} while( stopEnabled==0 ); // until jaer tells us to exit
	
	// close timer
	CUT_SAFE_CALL( cutStopTimer(timer));
	accTimer = cutGetTimerValue(timer);
	CUT_SAFE_CALL( cutDeleteTimer( timer));
	
	if(runCuda)	
		cudaClean();
	
#if !MEASUREMENT_MODE		
	printResults(fpLog);	
#endif

	fflush(stdout); // for jaer to get it
	return 0;

}

/**********************************************************************************************************************/
/*****************************************  MAIN FUNCTION  ************************************************************/
/**********************************************************************************************************************/
int main( int argc, char** argv)
{
#if !MEASUREMENT_MODE	
    fpLog = fopen("sim.log","w");
	if(!fpLog) {
		fprintf(stderr, "Cannot create simulation logging file sim.log in current directory\n");		
		exit(1);
	}

	/* Print out the date and time in the standard format. */
    /* Convert it to local time representation. */
    time_t curTime = time(NULL);
    struct tm *locTime;
	locTime = localtime (&curTime);     
    fprintf(fpLog, "==============================================================\n");
    fprintf(fpLog, "jAER-CUDA Simulation Log \n");
	fprintf(fpLog, "==============================================================\n");
    fputs (asctime (locTime), fpLog);
    fprintf(fpLog, "Delta time value : %d\n", delta_time);
	fflush(fpLog);    
	
#endif
	
	printf("starting jaercuda \n");
	runjaerCUDA( argc, argv);
	fflush(stdout);

#if !MEASUREMENT_MODE	
	fprintf(fpLog, "**END**\n");
	fclose(fpLog);
#endif

}

