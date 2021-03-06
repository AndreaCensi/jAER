#pragma NOIV               // Do not generate interrupt vectors since our interrupts are manually defined
//-----------------------------------------------------------------------------
//   File:      main.c
//   Description: FX2LP firmware for the CochleaAMS1c chip/board
//    
// cloned from cochleaAMS1b March 2011 by tobi
// created: 10/2008, cloned from DVS128 firmware stereo board firmware
// authors tobi delbruck, shih-chii liu, raphael berner
//
//-----------------------------------------------------------------------------

// changelog
// apr 2010 tobi changed clock polarity to end high

// if missing system headers, install the FX2LP development kit, which goes to C:\Cypress\...
// See 	  http://www.cypress.com/?rID=14321

#include <Fx2.h>
#include <fx2regs.h>
#include <syncdly.h>
#include "biasgen.h" 
#include "ports.h"
#include "portsFX2.h"
#include "micro.h"

extern BOOL GotSUD;             // Received setup data flag
//extern BOOL Sleep;
extern BOOL Rwuen;
extern BOOL Selfpwr;

//BYTE Configuration;             // Current configuration
//BYTE AlternateSetting;          // Alternate settings

//WORD packetSize;

#define TIMESTAMP_MASTER 		PC1
#define CFG_TIMESTAMP_COUNTER 	PC2
#define TIMESTAMP_MODE			PC3

#define DB_Addr 1 // zero if only one byte address is needed for EEPROM, one if two byte address

#define LED 	PA7

#define EEPROM_SIZE 0x8000
#define MAX_NAME_LENGTH 4
#define STRING_ADDRESS (EEPROM_SIZE - MAX_NAME_LENGTH)

#define MSG_TS_RESET 1

// vendor requests
#define VR_ENABLE_AE_IN 0xB3 // enable IN transfers
#define VR_DISABLE_AE_IN 0xB4 // disable IN transfers
#define VR_TRIGGER_ADVANCE_TRANSFER 0xB7 // trigger in packet commit (for host requests for early access to AE data) NOT IMPLEMENTED
#define VR_RESETTIMESTAMPS 0xBb 
#define VR_SET_DEVICE_NAME 0xC2
#define VR_TIMESTAMP_TICK 0xC3
#define VR_RESET_FIFOS 0xC4
#define VR_DOWNLOAD_CPLD_CODE 0xC5 
#define VR_READOUT_EEPROM 0xC9
#define VR_IS_TS_MASTER 0xCB
#define VR_MISSED_EVENTS 0xCC

#define VR_CONFIG 0xB8 // write bytes out to SPI to control on-chip biasgen, on-chip scanner, on-chip local gain, on-chip digital config, and off-chip DACs
				// the wLengthL field of SETUPDAT specifies the number of bytes to write out (max 64 per request)
				// the bytes are in the data packet
#define VR_SET_POWERDOWN 0xB9 // control powerDown. wValue controls the powerDown pin. Raise high to power off, lower to power on.
#define VR_EEPROM_BIASGEN_BYTES 0xBa // write bytes out to EEPROM for power on default

#define VR_SETARRAYRESET 0xBc // set the state of the array reset
#define VR_DOARRAYRESET 0xBd // toggle the array reset low long enough to reset all pixels. TCVS320 doesn't have this.

#define BIAS_FLASH_START 9 // start of bias value (this is where number of bytes is stored

#define	VR_UPLOAD		0xc0
#define VR_DOWNLOAD		0x40
#define VR_EEPROM		0xa2 // loads (uploads) EEPROM
#define	VR_RAM			0xa3 // loads (uploads) external ram

#define EP0BUFF_SIZE	0x40


/* port pin definitions

ports a,b,c,d are bit addressable, e is byte addressable

we have available and wired to CPLD the following ports as defined in the port assigment list for the lattice CPLD in 
C:\Users\tobi\Documents\~jAER-sourceForge\trunk\deviceFirmwarePCBLayout\LatticeMachXO\CochleaAMS1c\cochleaCPLD_FX2-portAssignement.txt

PC3-0
FD15-8 which is the same as PD7-0 if the FIFO are configured as byte-wide (WORDWIDE in all EPxFIFOCFG registers)
PE6-0 

following are sfr and sbit definitions from header files

sfr IOA     = 0x80;
sfr IOB		= 0x90
sfr IOC		= 0xA0 // bit addressable
sfr IOD     = 0xB0;  // port D (bit addressable also)
sfr IOE     = 0xB1;  // port E (only byte-addressable)


sfr OEA     = 0xB2;  // output enable, configures port pins, 0=input=default, 1=output
sfr OEB     = 0xB3;
sfr OEC     = 0xB4;
sfr OED     = 0xB5;
sfr OEE     = 0xB6;

sbit PA0=IOA^0;
sbit PA7=IOA^7;

sbit PB0=IOB^0;
sbit PB1=IOB^1;

sbit PC0=IOC^0;

sbit PD0=IOD^0; etc
*/

// set/clear bits in a register that is byte-addressable, e.g. port E, e.g. sb(IOE,BitOut)
#define sb(p,m) (((p)|=(m)));
#define cb(p,m) ((p)&=(~(m)));

// Port E is not bit-addressable. Therefore we define bitmasks of port E (IOE) here and use them later to define macros to set/clear these bits
// port E connections are in the schematics of the PCB and in the port assignments of the CPLD, where some ports are mapped through the CPLD

// from the port assigment readme file for the CPLD:
//  Line 18:	PowerdownxEO <= PE2xSI; // onchip masterbias shutdown
//	Line 19:   CochleaResetxRBO <= PE3xSI;	// cochlea logic reset
//	Line 20: 	CPLDReset <= PE7xsI; // cypress asserts this to reset CPLD

// from the PCB schematic:
// E0 bitOut from one chip shift registers
// E1 = bitLatch for onchip shift registers
// e2 = passed through CPLD, powerDown biasgen master bias
// e3 = passed through, cochlea logic reset
// e4 bitIn to onchip shift register for config etc
// e5 bitClock to onchip shift register
// e6 FXLED
// e7 ResetCPLD holds CPLD in reset until enumeration and host open happens


#define BitOut 	1	// selects data shift register path (bitIn, clock, latch)
#define BitLatch 	2	// selects channel selection shift register path
#define PowerDown 	4	// selects biasgen shift register path
#define ResetCochlea 	8	// a preamp feedback resistor selection bit
#define BitIn 	16	// another microphone preamp feedback resistor selection bit
#define BitClock		32	// (1) to reset latch states
#define FXLED		64	// Parallel (0) or Cascaded (1) Arch
#define ResetCPLD	128	// scanner sync output direct from cochleaams1c to fx2 (not through CPLD like others)

#define selectsMask 7 // 0000 0111 to select only select bits

#define powerDown() IOE|=Powerdown
#define powerUp() IOE&=(!Powerdown)

#define resetCochlea() IOE|=ResetCochlea
#define unresetCochlea() IOD&=(~ResetCochlea)

#define resetCPLD() IOE|=ResetCPLD
#define unresetCPLD() IOD&=(~ResetCPLD)

/*
 The clock should end up high, so that the slave shift register (SR) is powered.
 This is changed from original firmware which ended up clock low.
 Additional complication on cochleaAMS1b is that the clock is directed to the appropriate SRs 
 by the 3 select bits for the bias and local DACs.
 i.e., the clock to each SR chain is clock&select.
 When select goes low, then clock will go low too. Therefore, we have to end up with all selects set high, so that all clocks
 can be left high. Then when we load new data, we have to take other selects low, which will take their clocks low.
 We then load the data, toggle the latches, and leave clock high while setting other selects high.
 The selects are also anded.

*/
// Clocks one bit into one of the shift registers
#define clockOnce(); clock=0;  _nop_(); _nop_(); _nop_(); _nop_(); _nop_(); _nop_(); _nop_(); _nop_(); clock=1; // gives about 700n with 6 nops, which is needed on cochleaams1b because logic is not sized for speed
//#define clockOnce(); clock=1;  _nop_(); _nop_(); _nop_(); _nop_(); _nop_(); _nop_(); _nop_(); _nop_(); clock=0; // gives about 700n with 6 nops, which is needed on cochleaams1b because logic is not sized for speed

// latch input is 0=opaque, 1=transparent. toggleLatch latches the outputs of the shift registers.
#define toggleLatch() _nop_();  _nop_();  _nop_(); _nop_();  _nop_();  _nop_(); _nop_();  _nop_();  _nop_(); latch=1; _nop_();  _nop_();  _nop_(); _nop_();  _nop_();  _nop_(); _nop_();  _nop_();  _nop_(); latch=0; 



#define isScanSyncActive	(IOE&ScanSync==0)			// nonzero when scansync is active (bit has fallen out of scanner shift register). sync is active low

#define toggleVReset(); IOE|=Vreset; _nop_();_nop_();_nop_();_nop_();_nop_();_nop_(); IOE&=~Vreset;
//DataSel	C00-C04	bits for setting Iq of current-mode BPF
//			B00-B04	bits for setting Vq of SOS

// AddrSel is also used for selecting neuron that should be be loaded with KillBit,
// 8 neurons per channel, 4 neurons driven by IHC output, 4 neurons driven by bpf output
// chosen addr + Ybit=1 choses bpf neuron
// chosen addr + Ybit=1 choses bpf neuron

sbit tsReset=IOA^7;		// timestamp reset to CPLD
sbit runCPLD=IOA^3;		// runXs, run event acquisition


sbit cpldSRClk=IOC^1;		// CPLD config shift register
sbit cpldSRLatch=IOC^2;
sbit cpldSRBit=IOC^3;


sbit dacNSync=IOD^0;	// DAC start
sbit dacClock=IOD^1; 	// DAC clock
sbit dacBitIn=IOD^2; 	// DAC data

#define DATA_SEL (1<<3);
#define ADD_SEL (1<<4);
#define BIAS_SEL (1<<5);

sbit dataSel=IOD^3;
sbit addSel=IOD^4;
sbit biasgenSel=IOD^5; 


// following select the ipot, addr or data shiftregisters for input
// note these are changed from original notion so that all select are high normally (no one selected)
// and the other two go low when one is selected. This is so that the clock can be left high at the end as it should be

#define selectIPots IOD&=(~(DATA_SEL|ADD_SEL)) //  selects only biasgen select, turns off addr and data selects, leaves other bits untouched
#define selectAddr  IOD&=(~(DATA_SEL|BIAS_SEL)) // selects addr shifter, even addresses are left cochlea, odd addresses are right cochlea
#define selectData	IOD^3=(!(BIAS_SEL|ADD_SEL)) //  selects data shift register
#define selectNone	IOD|=(DATA_SEL|ADD_SEL|BIAS_SEL)			// raise all selects (yes, this is correct)


sbit scanClock=IOD^1;	// scanner clock
sbit yBit=IOD^2;	    // Chooses whether lpf (0) or bpf (1) neurons to be killed, use in conjunction with AddrSel and AERKillBit
sbit selAer=IOD^3;   	//Chooses whether lpf (0) or rectified (1) lpf output drives lpf neurons
sbit latch=IOD^4;		// onchip data latch

sbit aerKillBit=IOD^7;	// Set to (1) after Setting of AddrSel and Ybit to kill 4 neurons
//sbit dacNLDAC=IOD^7; // debug - board was hacked for this and removed

#define selectLPFKill yBit=0
#define selectBPFKill yBit=1

// was used for debug, nLDAC wired to ground on board #define toggleLDAC()  _nop_();  _nop_();  _nop_().; _nop_();  _nop_();  _nop_(); _nop_();  _nop_();  _nop_();  dacNLDAC=0; _nop_();  _nop_();  _nop_(); _nop_();  _nop_();  _nop_(); _nop_();  _nop_();  _nop_(); dacNLDAC=1;  // toggles LDAC after all 48 bits loaded and sync is high
#define	startDACSync() dacNSync=0; // starts DAC data input
#define endDACSync()	dacNSync=1; _nop_(); _nop_(); dacClock=1; // dacClock must go high *after* dacNSync goes high

unsigned int numBiasBytes; // number of bias bytes to send, used in loop
/*
// not using these at present
#define NUM_BIAS_BYTES 36
xdata unsigned char biasBytes[]={0x00,0x04,0x2B,\
								0x00,0x30,0x1C,\
								0xFF,0xFF,0xFF,\
								0x55,0x23,0xD4,\
								0x00,0x00,0x97,\
								0x06,0x86,0x4A,\
								0x00,0x00,0x00,\
								0xFF,0xFF,0xFF,\
								0x04,0x85,0x3D,\
								0x00,0x0E,0x28,\
								0x00,0x00,0x27,\
								0x00,0x00,0x04}; // bias bytes values saved here

*/
long cycleCounter;
//long missedEvents;

BOOL JTAGinit;

#define	I2C_Addr 0x50 //adress is 0101_0001

void startMonitor(void);
void stopMonitor(void);

void EEPROMRead(WORD addr, BYTE length, BYTE xdata *buf);
void EEPROMWrite(WORD addr, BYTE length, BYTE xdata *buf);
void EEPROMWriteBYTE(WORD addr, BYTE value);

void downloadSerialNumberFromEEPROM(void);
void initDAC();

//-----------------------------------------------------------------------------
// Task Dispatcher hooks
//   The following hooks are called by the task dispatcher.
//-----------------------------------------------------------------------------

void TD_Init(void)              // Called once at startup
{
	// set the CPU clock to 48MHz
	//CPUCS = ((CPUCS & ~bmCLKSPD) | bmCLKSPD1) ;
	CPUCS = 0x12 ; // 1_0010 : CLKSP1:0=10, cpu clockspeed 48MHz, drive CLKOUT output pin 100 which clocks CPLD

/*
(from raphael berner: the clocking is as follows:
the fx2 clockes the CPLD by the CLKOUT pin (pin 100), and the CPLD clocks
the fifointerface on IFCLK, so in the firmware you should select external
clocksource in the FX2 for the slave FIFO clock source.
*/

	IOC = 0x00; 
	IOA = 0x00;
	IOE=  0x00; // set port output default values - enable them as outputs next
	
	OEA = 0x8b; // 1000_1011. PA7 LED, PA3: nResetCPLD, PA1: runCPLD, PA0: tsReset   
				// port B is used as FD7-0 for 8 bit FIFO interface to CPLD
	OEC = 0x0F; // now are cochlea and offchip DAC controls, before was 0000_1101 // JTAG, timestampMode, timestampTick, timestampMaster, resetTimestamp
	OED	= 0xFF; // all bit addressable outputs, all WORDWIDE=0 so port d should be enabled
	OEE = 0x7F; // all outputs except scansync which is input, byte addressable

	// set the slave FIFO interface to 30MHz, slave fifo mode

	// select slave FIFO mode with with FIFO clock source as external clock (from CPLD).
	// if the CPLD is not programmed there will not be any FIFO clock!
	// if there is no IFCLK then the port D pins are never enabled as outputs.

	// start with internal clock, switch to external CPLD clock source at end of TD_Init
	SYNCDELAY;
	IFCONFIG = 0xA3; // 0000_0011   // external clock, 30MHz, don't drive clock IFCLKOE, slave FIFO mode
	SYNCDELAY; // may not be needed

	// disable interrupts by the input pins and by timers and serial ports. timer2 scanner interrupt enabled when needed from vendor request.
	IE &= 0x00; // 0000_0000 

	// disable interrupt pins 4, 5 and 6
	EIE &= 0xE3; // 1110_0011;

	// Registers which require a synchronization delay, see section 15.14
	// FIFORESET        FIFOPINPOLAR
	// INPKTEND         OUTPKTEND
	// EPxBCH:L         REVCTL
	// GPIFTCB3         GPIFTCB2
	// GPIFTCB1         GPIFTCB0
	// EPxFIFOPFH:L     EPxAUTOINLENH:L
	// EPxFIFOCFG       EPxGPIFFLGSEL
	// PINFLAGSxx       EPxFIFOIRQ
	// EPxFIFOIE        GPIFIRQ
	// GPIFIE           GPIFADRH:L
	// UDMACRCH:L       EPxGPIFTRIG
	// GPIFTRIG
  
	//disable all ports A,C,E alternate functions
	SYNCDELAY;
	PORTCCFG = 0x00;
	SYNCDELAY;
	PORTACFG = 0x00; // do not use interrupts 0 and 1
	SYNCDELAY;
	PORTECFG = 0x00;

	
	EP1OUTCFG = 0x00;			// EP1OUT disabled
	SYNCDELAY;
	EP1INCFG = 0xA0;			// 1010 0000 VALID+Bulk EP1IN enabled, bulk
	SYNCDELAY;                   
	EP2CFG = 0x00;				// EP2 disabled
	SYNCDELAY;                     
	EP4CFG = 0x00;				// EP4 disabled
	SYNCDELAY;                 
	EP6CFG = 0xE0;				// EP6 enabled, in bulk, quad buffered 
	SYNCDELAY;               
	EP8CFG = 0x00;				// EP8 disabled

	SYNCDELAY;
	REVCTL= 0x03;

	SYNCDELAY;
	FIFORESET = 0x80;
  	SYNCDELAY;
  	FIFORESET = 0x06;
  	SYNCDELAY;
  	FIFORESET = 0x00;
	SYNCDELAY;

	EP6AUTOINLENH=0x02;
	SYNCDELAY;
	EP6AUTOINLENL=0x00;

	SYNCDELAY;
	EP6FIFOCFG = 0x08 ; //0000_1000, autoin=1, wordwide=0 to automatically commit packets and make this an 8 bit interface to FD
	SYNCDELAY;
	EP2FIFOCFG = 0x00 ; // wordwide=0
	SYNCDELAY;
	EP4FIFOCFG = 0x00 ; 
	SYNCDELAY;
	EP8FIFOCFG = 0x00 ; 


	//set FIFO flag configuration: FlagB: EP6 full, flagC and D unused
	SYNCDELAY;
	PINFLAGSAB = 0xE8; // 1110_1000
	SYNCDELAY;


	cycleCounter=0;
//	missedEvents=0xFFFFFFFF; // one interrupt is generated at startup, maybe some cpld registers start in high state
	LED=1; // turn on LED

	clock=1; bitIn=0; latch=0; powerDown=0; // init biasgen ports and pins
	
	EZUSB_InitI2C(); // init I2C to enable EEPROM read and write


	JTAGinit=TRUE;	

  	IT0=1;		// make INT0# edge-sensitive
	EX0=0;		// do not enable INT0#

	IT1=1; // INT1# edge-sensitve
	EX1=0; // do not enable INT1#

	// timer2 init for scanner clocking in continuous mode
	T2CON=0x00; // 0000 0100 timer2 control, set to 16 bit with autoreload, timer stopped
	RCAP2L=0x00; // timer 2 low register loaded from vendor request.
	RCAP2H=0xFF;  // starting reload values, counter counts up to 0xFFFF from these and generates interrupt when count rolls to 0
	ET2=0; // disable interrupt to start

/* // not using now writing initial bias values
	for (i=0;i<NUM_BIAS_BYTES;i++)
	{
		spiwritebyte(biasBytes[i]);
	}
	latchNewBiases();	
*/
	toggleVReset();

	// now switch to external IFCLK for FIFOs
//	SYNCDELAY; // may not be needed
 //	IFCONFIG = 0x23; // 0010_0011  // extenal clock, slave fifo mode
//	SYNCDELAY; // may not be needed


	initDAC();

}

void TD_Poll(void)              // Called repeatedly while the device is idle
{ 	
	if(cycleCounter++>=100000){
		
		LED=!LED;	
		cycleCounter=0; // this makes a slow heartbeat with period of about 1s on the LED to show firmware is running
	}
}

/* The cochlea board has two 16 channel AD5391 DACs connected in daisy chain.
We need to load both of the two DACs each time we want to change the output from the
channel of one of them. To avoid using up FX2 RAM to remember the data (which we don't 
know initially to start with), the host
sends both DAC channels and values for changing just one of them. The other DAC just gets
a 3-byte 0,0,0 write which selects the SFRs with SFR 0 which is a nothing register.

Here we just clock through both of these holding sync low during the entire 48 bit load.
*/

//sends byte dat in big endian order out the DAC bit and clock outputs.  
// must leave clock low at end so that clock can be taken high *after* sync is taken high after all bytes clocked in
void sendDACByte(unsigned char b){
	unsigned char i=8;
	while(i--){
		dacClock=1;
		b=_crol_(b,1); // rotate left to get msb to lsb
		if(b&1){
			dacBitIn=1;
		}else{
			dacBitIn=0;
	   	}
		dacClock=0; // clk edge low while data stable
	}
}

// sends byte in big endian order to the CPLD for CPLD configuration
void sendCPLDByte(unsigned char b){
	unsigned char i=8;
	while(i--){
		cpldClk=1;
		b=_crol_(b,1); // rotate left to get msb to lsb
		if(b&1){
			cpldBitIn=1;
		}else{
			cpldBitIn=0;
	   	}
		cpldClk=0; // clk edge low while data stable
	}
}


/* implemented on host via SET_VDAC

void powerDownDAC(){
	startDACSync();

 	startDACSync();   

  	sendDACByte(0x08); 
	sendDACByte(0x00);
	sendDACByte(0x00);


	sendDACByte(0x08);
	sendDACByte(0x00);
	sendDACByte(0x00);  

	endDACSync();
}

void powerUpDAC(){
	startDACSync();

 	startDACSync();   

  	sendDACByte(0x09); 
	sendDACByte(0x00);
	sendDACByte(0x00);


	sendDACByte(0x09);
	sendDACByte(0x00);
	sendDACByte(0x00);  

	endDACSync();
}
*/
void initDAC()
{

/*
	dacNLDAC=0;
	EZUSB_Delay(30); 	// pause at least 10ms because scope shows that nBUSY stays low for about 8ms after power on. Specs say 270us for power on reset of DACs
	dacNLDAC=1;
	EZUSB_Delay(30);
*/
//   	dacNLDAC=0; 
	// AnB RnW 00 A3:0= 0000 1100 - configure Control register write,
	// REG1:0=00 SFRs
	// CR11=1 in power down hi z
	// CR10=1 internal reference is 2.5V
	// CR9=0  current boost on
	// CR8=1  internal reference used
	// CR7=0  monitor disabled
	// CR6=1  thermal monitor enabled
	// CR5:0=0 toggle disabled and using default A toggle, followed by 2 unused bits 00
	// entire config is
    // 0000 1100 0011 0101 0000 0000
	// AR00 addr RgCr             00
	//   0    c    3    5    0    0

 	startDACSync();   //' Trigger DAC. 

  	sendDACByte(0x0C); // send MSB first, sends big endian msb first
	sendDACByte(0x35);
	sendDACByte(0x00);


	sendDACByte(0x0C); // send MSB first, sends big endian msb first
	sendDACByte(0x35);
	sendDACByte(0x00);  

	endDACSync();

   }

// sends the byte out the 'spi' interface to the cochlea in big-endian order (msb first)
// - replaces assembly routine to use bit defines for clock and bitIn and C code
void sendConfigByte(unsigned char b){
	unsigned char i=8;
	while(i--){ // goes till i==0
		// rotate left to get msb, test bit to set bitin, then toggle clock high/low
		b=_crol_(b,1);
		if(b&1!=0){
			sb(=1;
		}else{
			bitIn=0;
		}
		clockOnce(); 
	}
}

// sends the nbits least significant bits in big-endian order, e.g. sendConfigBits(0xfe,3) sends 110 from 1111 1110
void sendConfigBits(unsigned char b,unsigned char nbits){
	unsigned char i=nbits; // send this many
	b=_crol_(b,8-nbits); // rotate to get msb of data to send in msb of b. if nbits=8 doesn't change b, if nbits=7, rotates left by 1
	while(nbits--){
		b=_crol_(b,1); // get the next bit in lsb
		if(b&1!=0){
			bitIn=1;
		}else{
			bitIn=0;
		}
		clockOnce();
	}
}

void downloadSerialNumberFromEEPROM(void)
{
	BYTE i;

	char *dscrRAM;
	BYTE xdata buf[MAX_NAME_LENGTH];

	// get pointer to string descriptor 3
	dscrRAM =  (char *)EZUSB_GetStringDscr(3);

	// read string description from EEPROM
	EEPROMRead(STRING_ADDRESS, MAX_NAME_LENGTH, buf);
	
	//write string description (serial number) to RAM
	for (i=0;i<MAX_NAME_LENGTH;i++)
	{
		dscrRAM[2+i*2] = buf[i];
	}
}

void startMonitor(void)
{
	nResetCPLD=1; //CPLD_NOT_RESET=1;
    runCPLD=1; //RUN_CPLD=1;
}

void stopMonitor(void)
{
    runCPLD=0; //RUN_CPLD=0;

  	// force last paket
  	
  	EP6FIFOCFG = 0x00; //0000_0000 disable auto-in
	SYNCDELAY;

	if(EP6FIFOFLGS==0x00)
	{ // if buffer available
    	INPKTEND=0x06; // force in paket
		SYNCDELAY;
	}

  	// reset fifo  	
  	FIFORESET = 0x80;
  	SYNCDELAY;
  	FIFORESET = 0x06;
  	SYNCDELAY;
  	FIFORESET = 0x00;
	SYNCDELAY;

	EP6FIFOCFG =0x08;  //0000_1000 set back to autoin
	SYNCDELAY;
}


void EEPROMWriteByte(WORD addr, BYTE value)
{
	BYTE		i = 0;
	BYTE xdata 	ee_str[3];
	if(DB_Addr)
		ee_str[i++] = MSB(addr); // if 16 bit, we need 2-byte address and 1 byte data

	ee_str[i++] = LSB(addr);
	ee_str[i++] = value;


	//EZUSB_WriteI2C(I2C_Addr, i, ee_str);
	// http://www.keil.com/forum/docs/thread11160.asp

	while( I2CPckt.status != I2C_IDLE );      // wait for write session
	while(EZUSB_WriteI2C( I2C_Addr, i, ee_str )!=I2C_OK);
 	EZUSB_WaitForEEPROMWrite( I2C_Addr );  // wait for Write Cycle Time
//	LED=1;
	
}

void EEPROMWrite(WORD addr, BYTE length, BYTE xdata *buf)
{
	BYTE	i;
	LED=!LED;
	for(i=0;i<length;++i)
		EEPROMWriteByte(addr++,buf[i]);
}

void EEPROMRead(WORD addr, BYTE length, BYTE xdata *buf)
{
	BYTE		i = 0;
	BYTE		j = 0;
	BYTE xdata 	ee_str[2];

	if(DB_Addr)
		ee_str[i++] = MSB(addr);

	ee_str[i++] = LSB(addr);

	EZUSB_WriteI2C(I2C_Addr, i, ee_str);

	for(j=0; j < length; j++)
		*(buf+j) = 0xcd;

	EZUSB_ReadI2C(I2C_Addr, length, buf);
}


BOOL TD_Suspend(void)          // Called before the device goes into suspend mode
{
  // reset CPLD
  nResetCPLD=0; //CPLD_NOT_RESET =0;  
  
  return(TRUE);
}

BOOL TD_Resume(void)          // Called after the device resumes
{
  // activate CPLD 
  nResetCPLD=1;    // CPLD_NOT_RESET=1; 

   return(TRUE);
}

//-----------------------------------------------------------------------------
// Device Request hooks
//   The following hooks are called by the end point 0 device request parser.
//-----------------------------------------------------------------------------

/*BOOL DR_GetDescriptor(void)
{
   return(TRUE);
}*/

BOOL DR_SetConfiguration(void)   // Called when a Set Configuration command is received
{
  if( EZUSB_HIGHSPEED( ) )
  { // FX2 enumerated at high speed
    SYNCDELAY;                  // 
    EP6AUTOINLENH = 0x02;       // set AUTOIN commit length to 512 bytes
    SYNCDELAY;                  // 
    EP6AUTOINLENL = 0x00;
    SYNCDELAY;                  
   // enum_high_speed = TRUE;
    }
  else
  { // FX2 enumerated at full speed
    SYNCDELAY;                   
    EP6AUTOINLENH = 0x00;       // set AUTOIN commit length to 64 bytes
    SYNCDELAY;                   
    EP6AUTOINLENL = 0x40;
    SYNCDELAY;                  
  //  enum_high_speed = FALSE;
  }

  //Configuration = SETUPDAT[2];
  return(TRUE);            // Handled by user code
}

BOOL DR_GetConfiguration(void)   // Called when a Get Configuration command is received
{
   EP0BUF[0] = 0x00;//Configuration;
   EP0BCH = 0;
   EP0BCL = 1;
   return(TRUE);            // Handled by user code
}

/*BOOL DR_SetInterface(void)       // Called when a Set Interface command is received
{
//   AlternateSetting = SETUPDAT[2];
   return(TRUE);            // Handled by user code
}*/

BOOL DR_GetInterface(void)       // Called when a Set Interface command is received
{
   EP0BUF[0] = 0x00;//AlternateSetting;
   EP0BCH = 0;
   EP0BCL = 1;
   return(TRUE);            // Handled by user code
}

/*BOOL DR_GetStatus(void)
{
   return(TRUE);
}

BOOL DR_ClearFeature(void)
{
   return(TRUE);
}

BOOL DR_SetFeature(void)
{
   return(TRUE);
}*/

// the SETUPDAT array has the following 8 elements (see FX2 TRM Section 2.3)
//SETUPDAT[0] VendorRequest 0x40 for OUT type, 0xC0 for IN type
//SETUPDAT[1] The actual vendor request (e.g. VR_ENABLE_AE_IN below)
//SETUPDAT[2] wValueL 16 bit value LSB
//         3  wValueH MSB
//         4  wIndexL 16 bit field, varies according to request
//         5  wIndexH
//         6  wLengthL Number of bytes to transfer if there is a data phase
//         7  wLengthH
BYTE xsvfReturn;

BOOL DR_VendorCmnd(void)
{	
	WORD value; 
	WORD len,ind, bc; // xdata used here to conserve data ram; if not EEPROM writes don't work anymore
/*
	union {
		unsigned short ushort;
		unsigned msb,lsb;
		unsigned bytes[2]; // big endian, bytes[0] is MSB as far as C51 is concerned
	} length;
*/
	WORD i;
	char *dscrRAM;
	unsigned char xdata JTAGdata[400];

	switch (SETUPDAT[1]){
		case VR_ENABLE_AE_IN: // enable IN transfers
			{
				startMonitor();
				break;  // handshake phase triggered below
			}
		case VR_DISABLE_AE_IN: // disable IN transfers
			{
				stopMonitor();
				break;
			}
		case VR_RESET_FIFOS: // reset in and out fifo
			{
				SYNCDELAY;
				EP6FIFOCFG = 0x00; //0000_0000  disable auto-in
				SYNCDELAY;
				FIFORESET = 0x80;
				SYNCDELAY;
				FIFORESET = 0x06;
				SYNCDELAY;
				FIFORESET = 0x00;


				SYNCDELAY;
				EP6FIFOCFG = 0x08 ; //0000_1000 reenable auto-in
				break;
			}
		case VR_DOWNLOAD_CPLD_CODE:
			{
			if (SETUPDAT[0]==VR_DOWNLOAD) {
				if (JTAGinit)
				{
					IOC=0x00;
					OEC = 0xBD;   // configure TDO (bit 6) and TSmaster as input  : 1011_1101
			
					xsvfInitialize();
					JTAGinit=FALSE;
					
				}

				len = SETUPDAT[6];
				len |= SETUPDAT[7] << 8;

				if (len>400)
				{
					xsvfReturn=10;
					OEC = 0x0D;   // configure JTAG pins to float : 0000_1111
					JTAGinit=TRUE;
					break;
				}

				value=0;

				resetReadCounter(JTAGdata);

				while(len)					// Move new data through EP0OUT 
				{							// one packet at a time.
					// Arm endpoint - do it here to clear (after sud avail)
					EP0BCH = 0;
					EP0BCL = 0; // Clear bytecount to allow new data in; also stops NAKing

					while(EP0CS & bmEPBUSY);

					bc = EP0BCL; // Get the new bytecount

					for(i=0; i<bc; i++)
							JTAGdata[value+i] = EP0BUF[i];							

					value += bc;
					len -= bc;
				}
			

				if (SETUPDAT[2]==0x00) //complete
				{
					OEC = 0x0D;   // configure JTAG pins to float : 0000_1111
					JTAGinit=TRUE;
				} else
				{
					xsvfReturn=xsvfRun();
					if (xsvfReturn>0) // returns true if error
					{
						OEC = 0x0D;   // configure JTAG pins to float : 0000_1101
						JTAGinit=TRUE;
				
					//	return TRUE;
					}

				}
	
				/* EP0BUF[0] = SETUPDAT[1];
				EP0BCH = 0;
				EP0BCL = 1;
				EP0CS |= bmHSNAK;

				return(FALSE); */
				break;
			}
 			else //case VR_XSVF_ERROR_CODE:
			{
				EP0BUF[0] = SETUPDAT[1];
				EP0BUF[1]= xsvfReturn;
				EP0BCH = 0;
				EP0BCL = 2;
				EP0CS |= bmHSNAK;

				return(FALSE);
			}
			}
		case VR_SET_DEVICE_NAME:
			{
				*EP0BUF = SETUPDAT[1];
				EP0BCH = 0;
				EP0BCL = 1;
				EP0CS |= bmHSNAK;

				while(EP0CS & bmEPBUSY); //wait for the data packet to arrive

				dscrRAM = (char*)EZUSB_GetStringDscr(3); // get address of serial number descriptor-string in RAM

				if (EP0BCL > MAX_NAME_LENGTH)
				{
					len=MAX_NAME_LENGTH;
				} else 
				{
					len=EP0BCL;
				}
	
				for (i=0;i<len;i++)
				{
					EEPROMWriteBYTE(STRING_ADDRESS+i, EP0BUF[i]); // write string to EEPROM
					dscrRAM[2+i*2] = EP0BUF[i]; // write string to RAM
				}

				for (i=len; i<MAX_NAME_LENGTH; i++) // fill the rest with stop characters
				{
					EEPROMWriteBYTE(STRING_ADDRESS+i, ' '); // write string to EEPROM				
					dscrRAM[2+i*2] = ' '; // write string to RAM
				}

				EP0BCH = 0;
				EP0BCL = 0;

				return(FALSE);
			}		
		case VR_RESETTIMESTAMPS:
			{
				tsReset=1; // RESET_TS=1; // assert RESET_TS pin for one instruction cycle (four clock cycles)
				tsReset=0; // RESET_TS=0;

				break;
			}
		case VR_CONFIG: // write bytes to SPI interface
		case VR_EEPROM_BIASGEN_BYTES: // falls through and actual command is tested below
			{
				// the value bytes are the specific config command
			 	// the index bytes are the arguments
				// more data comes in the setupdat
				
				SYNCDELAY;
				value = SETUPDAT[2];		// Get request value
				value |= SETUPDAT[3] << 8;	// data comes little endian
				ind = SETUPDAT[4];			// Get index
				ind |= SETUPDAT[5] << 8;
				len = SETUPDAT[6];      	// length for data phase
				len |= SETUPDAT[7] << 8;
				switch(value&0xFF){ // take LSB for specific setup command because equalizer uses MSB for channel
 
// from CochleaAMS1c.Biasgen java class 
#define CMD_IPOT  1
#define CMD_RESET_EQUALIZER  2
#define CMD_SCANNER  3
#define CMD_EQUALIZER 4
#define	CMD_SETBIT  5
#define CMD_VDAC  6
#define CMD_INITDAC 7
#define CMD_CPLDCONFIG 8

				case CMD_IPOT:
					selectIPots;

					numBiasBytes=len;
					while(len){	// Move new data through EP0OUT, one packet at a time, 
						// eventually will get len down to zero by bc=64,64,15 (for example)
						// Arm endpoint - do it here to clear (after sud avail)
						EP0BCH = 0;
						EP0BCL = 0; // Clear bytecount to allow new data in; also stops NAKing
						SYNCDELAY;
						while(EP0CS & bmEPBUSY);  // spin here until data arrives
						bc = EP0BCL; // Get the new bytecount
						for(i=0; i<bc; i++){
							sendConfigByte(EP0BUF[i]);
						}
//						value += bc;	// inc eeprom value to write to, in case that's what we're doing
						len -= bc; // dec total byte count
					}
					toggleLatch();
					selectNone;
					LED=!LED;
					break;

					
				case CMD_VDAC:
					// EP0BUF has b0=channel (same for each DAC), b1=DAC1 MSB, b2=DAC1 LSB, b3=DAC0 MSB, b4=DAC0 LSB
					if(len!=6) return TRUE; // error, should have 6 bytes which are just written out to DACs surrounded by dacNSync=0
					EP0BCH = 0;
					EP0BCL = 0; // Clear bytecount to allow new data in; also stops NAKing
					SYNCDELAY;
					while(EP0CS & bmEPBUSY);  // spin here until data arrives
					startDACSync();
					for(i=0;i<6;i++){
						sendDACByte(EP0BUF[i]);
					}
					endDACSync();
					//toggleLDAC();
					
					LED=!LED;
					break;
				case CMD_INITDAC:
					initDAC();
					LED=!LED;
					break;
				case CMD_SETBIT:
					EP0BCH = 0;
					EP0BCL = 0; // Clear bytecount to allow new data in; also stops NAKing
					SYNCDELAY;
					while(EP0CS & bmEPBUSY);  // spin here until data arrives
					// sends value=CMD_SETBIT, index=portbit with (port(b=0,d=1,e=2)<<8)|bitmask(e.g. 00001000) in MSB/LSB, byte[0]=value (1,0)
					// also if button is tristable type in GUI, then byte[0] has tristate in bit1
					{
						bit bitval=(EP0BUF[0]&1); // 1=set, 0=clear
						bit tristate=(EP0BUF[0]&2?1:0); // 1=tristate, 0=drive
						unsigned char bitmask=SETUPDAT[4]; // bitmaskit mask, LSB of ind
						switch(SETUPDAT[5]){ // this is port, MSB of ind
							case 0: // port a
								if(bitval) IOA|=bitmask; else IOA&= ~bitmask;
								if(tristate) OEA&= ~bitmask; else OEA|=bitmask; 
							break;
							case 1: // port c
								if(bitval) IOC|=bitmask; else IOC&= ~bitmask;
								if(tristate) OEC&= ~bitmask; else OEC|=bitmask; 
							break;
							case 2: // port d
								if(bitval) IOD|=bitmask; else IOD&= ~bitmask;
								if(tristate) OED&= ~bitmask; else OED|=bitmask; 
							break;
							case 3: // port e
								if(bitval) IOE|=bitmask; else IOE&= ~bitmask;
								if(tristate) OEE&= ~bitmask; else OEE|=bitmask; 
							break;
							default:
								return TRUE; // error
						}
						LED=!LED;
					}
					break;
				case CMD_SCANNER:
					// index=1, continuous, index=0 go to channel
					// Arm endpoint - do it here to clear (after sud avail) and get the data for channel to scan to if there is one. in any case must read data
					// or subsequent requests will fail.
					EP0BCH = 0;
					EP0BCL = 0; // Clear bytecount to allow new data in; also stops NAKing
					SYNCDELAY;
					while(EP0CS & bmEPBUSY);  // spin here until data arrives
					if(ind==0){ // go to channel
						ET2=0; // disable timer2 interrupt - IE.5
						TR2=0; // stop timer2
						i=255; // timeout on scanner clear
						while(IOE&ScanSync && i-->0){ // clock scanner to end and timeout if there is no chip there
							scanClock=1; // sync happens on falling edge
							_nop_();_nop_();_nop_();_nop_();_nop_();_nop_();_nop_();_nop_();_nop_();_nop_();
							scanClock=0;
							_nop_();_nop_();_nop_();_nop_();_nop_();_nop_();_nop_();_nop_();_nop_();_nop_();
						}
						if(i==0) return TRUE; // scan to start failed
						bc = EP0BUF[0]; // Get the channel number to scan to
						for(i=0; i<bc; i++){
							scanClock=1; _nop_();_nop_();_nop_();_nop_();_nop_();_nop_();_nop_();_nop_();_nop_();
							scanClock=0; _nop_();_nop_();_nop_();_nop_();_nop_();_nop_();_nop_();_nop_();_nop_();
						}
					}else{ // continuous scanning
						RCAP2L=0xff-EP0BUF[0];  // load timer 2 low byte reload register with 0xff-period. period=0 reload is 0xff00 (255 counts), period=255, reload is 0x0000, period=64k
						ET2=1; // enable timer2 interrupt - this is IE.5 bit addressable
						TR2=1; // run timer2
					}
					LED=!LED;
					break;
				case CMD_EQUALIZER:
/*
the scheme right now for loading the AERKillBit and the local Vq's go as follows,
start with AddSel, which has 7 bits, RX0 to RX6, toggle bitlatch low/high - this signal
latches the bits for the decoder.
The output of the decoder is not activated till DataSel is chosen, the 10 bits are loaded, 5 bits
for Vq of SOS and 5bits for Iq of bpf, then when bitlatch is toggled low/high, then the
output of the decoder is released.
During this toggle of latch, the selected channel will also latch in the value on AERKillBit.
The only thing that I'm worrying about right now is that this value has to be remembered somewhere,
i.e. if I choose channels 10, 15 neurons to be inactivated, then even if I choose
new values for Vq and Iq, this information has to be stored somewhere. The
AERKillBit in essence is like an additional bit to the bits for the DataSel.

*/
// value has cmd in LSB, channel in MSB
// index has b11=bpfkilled, b10=lpfkilled, b9-5=qbpf, b4-0=qsos
/*All other 16-bit and 32-bit values are stored, contrary to other Intel
processors, in big endian format, with the high-order byte stored first. For
example, the LJMP and LCALL instructions expect 16-bit addresses that are
in big endian format.
*/
//	index is channel address, bytes={gain,quality,killed (1=killed,0=active)}
					selectAddr;
					sendConfigBits(SETUPDAT[3],7); // send 7 bit address
					toggleLatch();
					_nop_();_nop_();_nop_();_nop_();_nop_();_nop_();_nop_();
					selectNone;
					_nop_();_nop_();_nop_();_nop_();_nop_();_nop_();_nop_();
					selectData;
					
					sendConfigBits(SETUPDAT[4]&0x1f,5);	   // what is this for?
					
					sendConfigBits((SETUPDAT[4]>>5)|(SETUPDAT[5]<<3),5);
/* commented out because of bug in cochleaams1b where select of a single kill bit is inverted so everybody but the one you want
is selected. however, the equalizer DAC current splitters still work
					
					// set each killbit
					selectLPFKill; // clears ybit
					if(SETUPDAT[5]&4){ // kill LPF						
						aerKillBit=0; // hack
					}else{
						aerKillBit=0;
					}
					toggleLatch();
					
					selectBPFKill; // sets ybit
					if(SETUPDAT[5]&8){ // kill BPF						
						aerKillBit=0; // hack
					}else{
						aerKillBit=0;
					}
*/
					toggleLatch();
					selectNone;

					LED=!LED;


					break;
				case CMD_RESET_EQUALIZER:
					return TRUE;  // not yet implmented
					LED=!LED;
					break;

				case CMD_CPLDCONFIG: // send bit string to CPLD configuration shift register (new feature on cochleaAMS1c board/cpld/firmware)
					// len holds the number of bytes to send
					EP0BCH = 0;
					EP0BCL = 0; // Clear bytecount to allow new data in; also stops NAKing
					SYNCDELAY;
					while(EP0CS & bmEPBUSY);  // spin here until data arrives
					
					for(i=0;i<len;i++){ // send out each byte of cpld config
						sendCPLDByte(EP0BUF[i]);
					}
										
					LED=!LED;
					break;

				default:
					return(TRUE);  // don't recognize command, generate stall
				}
				EP0BCH = 0;
				EP0BCL = 0;                   // Arm endpoint with 0 byte to transfer
				return(FALSE); // FALSE means no error - very important, otherwise get stall
			}
		case VR_SET_POWERDOWN: // control powerDown output bit
			{
				if (SETUPDAT[2])
				{
					powerDown=1;
				} else 
				{
					powerDown=0;
				}
				*EP0BUF=VR_SET_POWERDOWN;
				SYNCDELAY;
				EP0BCH = 0;
				EP0BCL = 1;                   // Arm endpoint with 1 byte to transfer
				SYNCDELAY;
				EP0CS |= bmHSNAK;             // Acknowledge handshake phase of device request
				break; // very important, otherwise get stall
			}
/*
		case VR_SETARRAYRESET: // set array reset, based on lsb of argument
			{
				if (SETUPDAT[2]&0x01)
				{
					IOE=IOE|ARRAY_RESET_MASK; //IOE|=arrayReset;
				} else
				{
					IOE=IOE&NOT_ARRAY_RESET_MASK; 
				}
			
				*EP0BUF=VR_SETARRAYRESET;
				SYNCDELAY;
				EP0BCH = 0;
				EP0BCL = 1;                   // Arm endpoint with 1 byte to transfer
				EP0CS |= bmHSNAK;             // Acknowledge handshake phase of device request
				return(FALSE); // very important, otherwise get stall

			}
		case VR_DOARRAYRESET: // reset array for fixed reset time
			{
				IOE=IOE&NOT_ARRAY_RESET_MASK; 
				_nop_();
				_nop_();
				_nop_();
				_nop_();
				_nop_();
				_nop_();
				_nop_();
				_nop_();	// a few us
				_nop_();
				_nop_();
				_nop_();
				_nop_();
				_nop_();
				_nop_();
				IOE=IOE|ARRAY_RESET_MASK; //IOE|=arrayReset;
				*EP0BUF=VR_DOARRAYRESET;
				SYNCDELAY;
				EP0BCH = 0;
				EP0BCL = 1;                   // Arm endpoint with 1 byte to transfer
				EP0CS |= bmHSNAK;             // Acknowledge handshake phase of device request
				return (FALSE); // very important, otherwise get stall
			}
*/
/*	case VR_TIMESTAMP_TICK:
			{
				if (SETUPDAT[0]==VR_UPLOAD) //1010_0000 :vendor request to device, direction IN
				{
					EP0BUF[0] = SETUPDAT[1];
				
					EP0BUF[1]= operationMode;
					
					EP0BCH = 0;
					EP0BCL = 2;
					EP0CS |= bmHSNAK;
				} else
				{
					operationMode=SETUPDAT[2];
					if (operationMode==0)
					{
						TIMESTAMP_MODE = 0;
						CFG_TIMESTAMP_COUNTER = 0;
					}else if (operationMode==1)
					{
  						CFG_TIMESTAMP_COUNTER = 1;
						TIMESTAMP_MODE = 0;	
					}else if (operationMode==2)
					{
  						CFG_TIMESTAMP_COUNTER = 0;
						TIMESTAMP_MODE = 1;	
					}else if (operationMode==3)
					{
  						CFG_TIMESTAMP_COUNTER = 1;
						TIMESTAMP_MODE = 1;	
					}

					*EP0BUF = SETUPDAT[1];
					EP0BCH = 0;
					EP0BCL = 1;
					EP0CS |= bmHSNAK;	
				}
				return(FALSE);
			}*/
		case VR_IS_TS_MASTER:
			{
				EP0BUF[0] = SETUPDAT[1];
				EP0BUF[1]= TIMESTAMP_MASTER;
				EP0BCH = 0;
				EP0BCL = 2;
				EP0CS |= bmHSNAK;

				return(FALSE);
			}
	/*	case VR_MISSED_EVENTS:
			{
				EX1=0;
				EP0BUF[0] = SETUPDAT[1];
				EP0BUF[4]= (missedEvents & 0xFF000000) >> 24;
				EP0BUF[3]= (missedEvents & 0x00FF0000) >> 16;
				EP0BUF[2]= (missedEvents & 0x0000FF00) >> 8;
				EP0BUF[1]= missedEvents & 0x000000FF;
				EP0BCH = 0;
				EP0BCL = 5;
				EP0CS |= bmHSNAK;

				missedEvents=0;
				EX1=1;
				return(FALSE);
			}*/
		case VR_RAM:
		case VR_EEPROM:
		{
			value = SETUPDAT[2];		// Get address and length
			value |= SETUPDAT[3] << 8;
			len = SETUPDAT[6];
			len |= SETUPDAT[7] << 8;
			// Is this an upload command ?
			if(SETUPDAT[0] == VR_UPLOAD)  // this is automatically defined on host from direction of vendor request
			{
				while(len)					// Move requested data through EP0IN 
				{							// one packet at a time.

					while(EP0CS & bmEPBUSY);

					if(len < EP0BUFF_SIZE)
						bc = len;
					else
						bc = EP0BUFF_SIZE;

					// Is this a RAM upload ?
					if(SETUPDAT[1] == VR_RAM)
					{
						for(i=0; i<bc; i++)
							*(EP0BUF+i) = *((BYTE xdata *)value+i);
					}
					else
					{
						for(i=0; i<bc; i++)
							*(EP0BUF+i) = 0xcd;
						EEPROMRead(value,(WORD)bc,(WORD)EP0BUF);
					}

					EP0BCH = 0;
					EP0BCL = (BYTE)bc; // Arm endpoint with # bytes to transfer

					value += bc;
					len -= bc;

				}
			}
			// Is this a download command ?
			else if(SETUPDAT[0] == VR_DOWNLOAD) // this is automatically defined on host from direction of vendor request
			{
				while(len)					// Move new data through EP0OUT 
				{							// one packet at a time.
					// Arm endpoint - do it here to clear (after sud avail)
					EP0BCH = 0;
					EP0BCL = 0; // Clear bytecount to allow new data in; also stops NAKing

					while(EP0CS & bmEPBUSY);

					bc = EP0BCL; // Get the new bytecount

					// Is this a RAM download ?
					if(SETUPDAT[1] == VR_RAM)
					{
						for(i=0; i<bc; i++)
							*((BYTE xdata *)value+i) = *(EP0BUF+i);
					}
					else
						EEPROMWrite(value,bc,(WORD)EP0BUF);

					value += bc;
					len -= bc;
				}
			}
			return(FALSE);
		}
		default:
		{ // we received an invalid command
			return(TRUE);
		}
	}

	*EP0BUF = SETUPDAT[1];
	EP0BCH = 0;
	EP0BCL = 1;
	EP0CS |= bmHSNAK;

	return(FALSE);
}

// a single timer interrupt is used for clocking the scanner

// RESET HOST TIMESTAMP INTERRUPT not used
void ISR_scannerClock(void) interrupt 5 { // interrupt vector address is 0x2b from fx2 manual which is interrupt 5 from c51 manual
	EA=0; // disable all interrupts
	TF2=0; // must clear or we come straight back here.
	scanClock=1;
	_nop_();
	_nop_();
	_nop_();
	_nop_();
	_nop_(); // 5 nops gives about 0.6us high time
	scanClock=0;
	EA=1;
}

/*
void ISR_MissedEvent(void) interrupt 3 {	
	missedEvents++;
}
*/

//-----------------------------------------------------------------------------
// USB Interrupt Handlers
//   The following functions are called by the USB interrupt jump table.
//-----------------------------------------------------------------------------

// Setup Data Available Interrupt Handler
void ISR_Sudav(void) interrupt 0
{
   GotSUD = TRUE;            // Set flag
   EZUSB_IRQ_CLEAR();
   USBIRQ = bmSUDAV;         // Clear SUDAV IRQ
}

// Setup Token Interrupt Handler
void ISR_Sutok(void) interrupt 0
{
   EZUSB_IRQ_CLEAR();
   USBIRQ = bmSUTOK;         // Clear SUTOK IRQ
}

void ISR_Sof(void) interrupt 0
{
   EZUSB_IRQ_CLEAR();
   USBIRQ = bmSOF;            // Clear SOF IRQ
}

void ISR_Ures(void) interrupt 0
{
   if (EZUSB_HIGHSPEED())
   {
      pConfigDscr = pHighSpeedConfigDscr;
      pOtherConfigDscr = pFullSpeedConfigDscr;
    //  packetSize = 512;

   }
   else
   {
      pConfigDscr = pFullSpeedConfigDscr;
      pOtherConfigDscr = pHighSpeedConfigDscr;
    //  packetSize = 64;
   }
   
   EZUSB_IRQ_CLEAR();
   USBIRQ = bmURES;         // Clear URES IRQ
}

void ISR_Susp(void) interrupt 0
{
//   Sleep = TRUE;
//   EZUSB_IRQ_CLEAR();
//   USBIRQ = bmSUSP;
}

void ISR_Highspeed(void) interrupt 0
{
   if (EZUSB_HIGHSPEED())
   {
      pConfigDscr = pHighSpeedConfigDscr;
      pOtherConfigDscr = pFullSpeedConfigDscr;
    //  packetSize = 512;

   }
   else
   {
      pConfigDscr = pFullSpeedConfigDscr;
      pOtherConfigDscr = pHighSpeedConfigDscr;
    //  packetSize = 64;
   }

   EZUSB_IRQ_CLEAR();
   USBIRQ = bmHSGRANT;
}
void ISR_Ep0ack(void) interrupt 0
{
}
void ISR_Stub(void) interrupt 0
{
}
void ISR_Ep0in(void) interrupt 0
{
}
void ISR_Ep0out(void) interrupt 0
{
}
void ISR_Ep1in(void) interrupt 0
{
}
void ISR_Ep1out(void) interrupt 0
{
}
void ISR_Ep2inout(void) interrupt 0
{
}
void ISR_Ep4inout(void) interrupt 0
{
}
void ISR_Ep6inout(void) interrupt 0
{
}
void ISR_Ep8inout(void) interrupt 0
{
}
void ISR_Ibn(void) interrupt 0
{
}
void ISR_Ep0pingnak(void) interrupt 0
{
}
void ISR_Ep1pingnak(void) interrupt 0
{
}
void ISR_Ep2pingnak(void) interrupt 0
{
}
void ISR_Ep4pingnak(void) interrupt 0
{
}
void ISR_Ep6pingnak(void) interrupt 0
{
}
void ISR_Ep8pingnak(void) interrupt 0
{
}
void ISR_Errorlimit(void) interrupt 0
{
}
void ISR_Ep2piderror(void) interrupt 0
{
}
void ISR_Ep4piderror(void) interrupt 0
{
}
void ISR_Ep6piderror(void) interrupt 0
{
}
void ISR_Ep8piderror(void) interrupt 0
{
}
void ISR_Ep2pflag(void) interrupt 0
{
}
void ISR_Ep4pflag(void) interrupt 0
{
}
void ISR_Ep6pflag(void) interrupt 0
{
}
void ISR_Ep8pflag(void) interrupt 0
{
}
void ISR_Ep2eflag(void) interrupt 0
{
}
void ISR_Ep4eflag(void) interrupt 0
{
}
void ISR_Ep6eflag(void) interrupt 0
{
}
void ISR_Ep8eflag(void) interrupt 0
{
}
void ISR_Ep2fflag(void) interrupt 0
{
}
void ISR_Ep4fflag(void) interrupt 0
{
}
void ISR_Ep6fflag(void) interrupt 0
{
}
void ISR_Ep8fflag(void) interrupt 0
{
}
void ISR_GpifComplete(void) interrupt 0
{
}
void ISR_GpifWaveform(void) interrupt 0
{
}
