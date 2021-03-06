/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package kalmanfilter;

/**
 *
 * @author Eero
 */
public class KalmanFilter {
    /* Kalman filter parameters:*/
    protected double[][] At;
    protected double[][] AtT;
    protected double[][] Bt;
    protected double[][] Ct;
    protected double[][] CtT;

    protected double[][] mu;
    protected double[][] Sigma;
    protected double[][] muBel;
    protected double[][] SigmaBel;
    protected double dt;
    protected double[][] Kt;
    //protected double[][] Rt;
    //protected double[][] Qt;

    /* Auxiliary matrices used for intermediate results:*/
    protected double[][] Mnn1; //n*n, i.e., the size of At
    protected double[][] Mnn2; //n*n, i.e., the size of At
    protected double[][] Mnk1; //n*k, i.e., the size of Kt and CtT
    protected double[][] Mnk2; //n*k, i.e., the size of Kt and CtT
    protected double[][] Mkk1; //k*k, i.e., the size of Qt
    protected double[][] Mkk2; //k*k, i.e., the size of Qt

    protected double[][] vn1; //n*1, i.e., the size of mu
    protected double[][] vn2; //n*1, i.e., the size of mu
    protected double[][] vk1; //k*1, i.e., the size of meas
    protected double[][] vk2; //k*1, i.e., the size of meas


    public KalmanFilter(double[][] At_, double[][] Bt_, double[][] mu_, double[][] Sigma_, double dt_, double[][] Kt_){
    At = At_;
    Bt = Bt_;
    Kt = Kt_;
    mu = mu_;
    Sigma = Sigma_;
    dt = dt_;
    }

    public KalmanFilter(){}

    public void updateMuBel(double[][] act){ //act is m*1 matrix
        matrixMultiplication(At,mu,vn1);
        matrixMultiplication(Bt,act,vn2);
        matrixSum(vn1,vn2,muBel);
    }

    public void updateSigmaBel(double[][] Rt){
        matrixMultiplication(At, Sigma, Mnn1);
        matrixMultiplication(Mnn1,AtT,Mnn2);
        matrixSum(Mnn2,Rt,SigmaBel);
    }

    public void updateKalmanGain(double[][] Qt){
        matrixMultiplication(SigmaBel, CtT, Mnk1);
        matrixMultiplication(Ct,Mnk1,Mkk1);
        matrixSum(Mkk1,Qt,Mkk2);
        invert2by2Matrix(Mkk2,Mkk1); //assuming M2 is a 2*2 matrix
        matrixMultiplication(Mnk1,Mkk1,Kt);
    }

    public void updateMu(double[][] meas){ //meas is k*1 matrix
        matrixMultiplication(Ct,muBel,vk1);
        matrixSubstraction(meas,vk1,vk2);
        matrixMultiplication(Kt,vk2,vn1);
        matrixSum(muBel,vn1,mu);
    }

    public void updateSigma(){
        matrixMultiplication(Kt,Ct,Mnn1);
        matrixMultiplication(Mnn1,SigmaBel,Mnn2);
        matrixSubstraction(SigmaBel,Mnn2,Sigma);
    }

    public void updateFilter(double[][] act, double[][] meas, double[][] Rt, double[][] Qt){
        updateMuBel(act);
        updateSigmaBel(Rt);
        updateKalmanGain(Qt);
        updateMu(meas);
        updateSigma();
    }

    public static void matrixCopy(double[][] A, double[][] R){
        int Arow = A[0].length;
        int Acol = A.length;

        for(int i=0; i<Acol; i++){
            for(int j=0; j<Arow; j++){
                R[i][j] = A[i][j];
            }
        }
    }

    public static void matrixSum(double[][] A, double[][] B, double[][] R){ /**
                                                                             * result: R=A+B
     */
        int Arow = A[0].length;
        int Acol = A.length;

        for(int i=0; i<Acol; i++){
            for(int j=0; j<Arow; j++){
                R[i][j] = A[i][j]+B[i][j];
            }
        }
    }

        public static void matrixSubstraction(double[][] A, double[][] B, double[][] R){ /**
                                                                             * result: R=A-B
     */
        int Arow = A[0].length;
        int Acol = A.length;

        for(int i=0; i<Acol; i++){
            for(int j=0; j<Arow; j++){
                R[i][j] = A[i][j]-B[i][j];
            }
        }
    }


    public static void matrixMultiplication(double[][] A, double[][] B, double[][] R){ /**
    result: R=A*B */
    int Arow = A[0].length;
    int Acol = A.length;

    int Brow = B[0].length;
    int Bcol = B.length;

    for(int i=0; i<Acol; i++){
        for(int j=0; j<Brow; j++){
            for(int k=0; k<Arow; k++){
                R[i][j] += A[i][k]*B[k][j];
            }
        }
    }
}

    public static void upperTriangularMatrixMultiplication(double[][] A, double[][] B, double[][] R){ /**
    A is an upper triangular matrix, result: R=A*B */
    int Arow = A[0].length;
    int Acol = A.length;

    int Brow = B[0].length;
    int Bcol = B.length;

    for(int i=0; i<Acol; i++){
        for(int j=0; j<Brow; j++){
            for(int k=i; k<Arow; k++){
                R[i][j] += A[i][k]*B[k][j];
            }
        }
    }
}

    public static void lowerTriangularMatrixMultiplication(double[][] A, double[][] B, double[][] R){ /**
    B is a lower triangular matrix, result: R=A*B */
    int Arow = A[0].length;
    int Acol = A.length;

    int Brow = B[0].length;
    int Bcol = B.length;

    for(int i=0; i<Acol; i++){
        for(int j=0; j<Brow; j++){
            for(int k=0; k<=i; k++){
                R[i][j] += A[i][k]*B[k][j];
            }
        }
    }
}

    public static void transposeMatrix(double[][] A, double[][] R){/** result: R = transpose(A)*/
        int Arow = A[0].length;
        int Acol = A.length;

        for(int i=0; i<Acol; i++){
            for(int j=0; j<Arow; j++){
                R[i][j] = A[j][i];
            }
        }
    }

    public static void invert2by2Matrix(double[][] A, double[][] R){/**
                                                                 * A is 2 by 2 matrix,
                                                                 * result: R = inv (A) Ü
                                                                 * if det(A) != 0, else R = 0
     */
    double detA = A[0][0]*A[1][1]-A[0][1]*A[1][0];
    if(detA == 0){
         R[0][0] = 0;
         R[0][1] = 0;
         R[1][0] = 0;
         R[1][1] = 0;
    }
    else{
    R[0][0] = (1/detA)*A[1][1];
    R[0][1] = -(1/detA)*A[0][1];
    R[1][0] = -(1/detA)*A[1][0];
    R[1][1] = (1/detA)*A[0][0];
    }
    }

}
