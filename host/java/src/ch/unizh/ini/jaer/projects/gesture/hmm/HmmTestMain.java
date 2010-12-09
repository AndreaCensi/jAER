package ch.unizh.ini.jaer.projects.gesture.hmm;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.util.HashSet;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTextField;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Jun Haeng Lee
 */
public class HmmTestMain
{
    /**
     *
     * @param args
     */
    public static void main(String[] args)
    {
//        bob_Alice(); // Bob and Alice example in Wikipedia
//        testViterbi(); // Tests Viterbi algorithm
//        testBaumWelch(); // Tests Baum-Welch learning
//        testSilentState(); // Tests forward, backward, and Viterbi algorithm with silent states, Currently, Baum-Welch method does not support silent states.
//        testGesture1(); // Tests the performance of gesture recognition module. All possible observations are scanned.
//        testDynamicThreshold(); // Tests dynamic threshold model
//        testGesture3(); // test partial gesture
        testGestureWithHandDrawing(); // Tests a HMM based gesture recognition system using hand drawing panel
    }

    /**
     * tests a HMM based gesture recognition system using hand drawing panel
     * Gestures are generated by mouse movements.
     */
    public static void testGestureWithHandDrawing(){
        String [] bNames = {"Remove", "Add", "Show", "Reset", "Learn", "Guess"};
        new HandDrawingTest("HMM based gesture recognition test using hand drawing panel", bNames);
    }

    /**
     * Class for testGestureWithHandDrawing()
     */
    static class HandDrawingTest extends TrajectoryDrawingPanel implements ItemListener{
        /**
         * Button names
         */
        public final String REMOVE = "Remove";
        public final String ADD = "Add";
        public final String SHOW = "Show";
        public final String RESET = "Reset";
        public final String LEARN = "Learn";
        public final String GUESS = "Guess";

        /**
         * Optional HMM models
         */
        public final String ERGODIC = "ERGODIC";
        public final String LR = "LR";
        public final String LRB = "LRB";
        public final String LRC = "LRC";
        public final String LRBC = "LRBC";

        /**
         * Stores gesture names in a set to guarantee the uniqueness of names
         */
        public HashSet<String> gestureItems = new HashSet<String>();

        JComboBox gestureChoice;
        JComboBox hmmModelChoice;
        JTextField newGesture;

        /**
         * All gestures have the same number of states.
         */
        int numState = 5;

        /**
         *  Feature vector space consists of 16 quantized vectors.
         */
        String[] featureVectorSpace = new String[] {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15"};

        /**
         * use dynamic threshold model. If you set 'false' instead of 'true', you can use a static threshold model.
         */
        GestureHmm ghmm = new GestureHmm(featureVectorSpace, GestureHmm.GAUSSIAN_THRESHOLD|GestureHmm.DYNAMIC_THRESHOLD, 16);
        
        /**
         * 16 observations are extracted from 16 directions feature vector space.
         */
        FeatureExtraction fve = new FeatureExtraction(16, 16);

        /**
         * Output statement buffer
         */
        String msg = "";

        /**
         * constructor
         * @param title
         * @param buttonNames
         */
        public HandDrawingTest(String title, String[] buttonNames) {
            super(title, 600, 600, buttonNames);
        }

        /**
         * Composes the button layout of Drawing Window Frame
         * 
         * @param componentNames
         */
        @Override
        public void buttonLayout(String[] componentNames) {
            gestureChoice = new JComboBox();
            gestureChoice.setName("gestureChoice");
            gestureChoice.addItem("Select a gesture");
            hmmModelChoice = new JComboBox();
            hmmModelChoice.setName("hmmModelChoice");
            hmmModelChoice.addItem("Select HMM model");
            hmmModelChoice.addItem(ERGODIC);
            hmmModelChoice.addItem(LR);
            hmmModelChoice.addItem(LRB);
            hmmModelChoice.addItem(LRC);
            hmmModelChoice.addItem(LRBC);
            newGesture = new JTextField();
            newGesture.setText("New gesture name");

            // configuration of button panel
            buttonPanel.setLayout(new GridLayout(2, (componentNames.length+3)/2));

            // adds gesture choice
            buttonPanel.add(gestureChoice, "1");
            gestureChoice.addItemListener(this);

            // adds new gesture name
            buttonPanel.add(newGesture, "2");

            // adds HMM model choice
            buttonPanel.add(hmmModelChoice, "3");
            hmmModelChoice.addItemListener(this);

            // adds buttons
            JButton newButton;
            for(int i = 0; i< componentNames.length; i++){
                newButton = new JButton(componentNames[i]);
                buttonPanel.add(newButton, ""+(i+4));
                newButton.addActionListener(buttonActionListener);
            }
            JButton clearButton = new JButton(clearButtonName);
            buttonPanel.add(clearButton, ""+ (componentNames.length + 4));
            clearButton.addActionListener(buttonActionListener);
        }


        /**
         * defines the actions caused by button pressed.
         * @param buttonName
         */
        @Override
        public void buttonAction(String buttonName) {
            if(buttonName.equals(LEARN)){
                doLearn();
                clearImage();
            } else if(buttonName.equals(ADD)){
                doAddGesture();
            } else if(buttonName.equals(REMOVE)){
                doRemoveGesture();
            } else if(buttonName.equals(GUESS)){
                doGuess();
//                clearImage();
            } else if(buttonName.equals(RESET)){
                doReset();
                clearImage();
            } else if(buttonName.equals(SHOW)){
                doShow();
            }
        }

        @Override
        public void menuAction(String menuName) {
            // do nothing
        }



        /**
         * excutes Remove button
         */
        public void doRemoveGesture(){
            String gesName = (String)gestureChoice.getSelectedItem();
            if(gesName == null || gesName.equals("") || gesName.equals("Select a gesture")){
                System.out.println("Warning: Gesture is not selected.");
                return;
            }

            ghmm.removeGesture(gesName);
            gestureChoice.removeItem(gesName);
            gestureItems.remove(gesName);
            System.out.println(gesName + " was removed.");
        }

        /**
         * excutes Add button
         */
        public void doAddGesture(){
            String newGestName = newGesture.getText();
            if(newGestName.equals("")){
                System.out.println("Warning: Gesture name is not specified.");
                return;
            }

            if(((String) hmmModelChoice.getSelectedItem()).startsWith("Select HMM model")) {
                System.out.println("Warning: HMM model is not specified.");
                return;
            }

            String gestName = newGestName+"_"+hmmModelChoice.getSelectedItem();

            if(!gestureItems.contains(gestName)){
                gestureItems.add(gestName);
                gestureChoice.addItem(gestName);
                HiddenMarkovModel.ModelType selectedModel;
                if(hmmModelChoice.getSelectedItem().equals("ERGODIC"))
                    selectedModel = HiddenMarkovModel.ModelType.ERGODIC_RANDOM;
                else if(hmmModelChoice.getSelectedItem().equals("LR"))
                    selectedModel = HiddenMarkovModel.ModelType.LR_RANDOM;
                else if(hmmModelChoice.getSelectedItem().equals("LRB"))
                    selectedModel = HiddenMarkovModel.ModelType.LRB_RANDOM;
                else if(hmmModelChoice.getSelectedItem().equals("LRC"))
                    selectedModel = HiddenMarkovModel.ModelType.LRC_RANDOM;
                else if(hmmModelChoice.getSelectedItem().equals("LRBC"))
                    selectedModel = HiddenMarkovModel.ModelType.LRBC_RANDOM;
                else{
                    System.out.println("Warning: Failed to add a new gesture.");
                    return;
                }

                ghmm.addGesture(gestName, numState,  selectedModel);
                ghmm.initializeGestureRandom(gestName);

                System.out.println("A new gesture ("+ gestName + ") is added.");
            }
            gestureChoice.setSelectedItem(gestName);
            newGesture.setText("");
        }

        /**
         * excutes Learn button
         */
        public void doLearn(){
            String gesName = (String) gestureChoice.getSelectedItem();
            if(gesName == null || gesName.equals("") || gesName.equals("Select a gesture")){
                System.out.println("Warning: Gesture is not selected.");
                return;
            }
            
            String[] fv = fve.convTrajectoryToCodewords(trajectory, -1);
            if(fv[0] == null){
                System.out.println("Warning: No trajectory is dected.");
                return;
            }
            System.out.println("Learning " + gesName);

            boolean learningSuccess;
            HiddenMarkovModel.ModelType modelType = ghmm.getGestureHmm(gesName).getModelType();

            // for LRC & LRBC, we don't have to update start probability
            if(modelType == HiddenMarkovModel.ModelType.LRC_RANDOM ||  modelType == HiddenMarkovModel.ModelType.LRBC_RANDOM)
                learningSuccess = ghmm.learnGesture(gesName, fv, fve.vectorAngleSeq, false, true, true);
            else
                learningSuccess = ghmm.learnGesture(gesName, fv, fve.vectorAngleSeq, true, true, true);

            if(learningSuccess){
                if(ghmm.getGestureHmm(gesName).getNumTraining() == 1)
                    System.out.println(gesName+" is properly registered. Log{P(O|model)} = " + Math.log10(ghmm.getGestureLikelyhood(gesName, fv)));
                else if(ghmm.getGestureHmm(gesName).getNumTraining() == 2)
                    System.out.println(gesName+" has been trained twice. Log{P(O|model)} = " + Math.log10(ghmm.getGestureLikelyhood(gesName, fv)));
                else
                    System.out.println(gesName+" has been trained " + ghmm.getGestureHmm(gesName).getNumTraining() + " times. Log{P(O|model)} = " + Math.log10(ghmm.getGestureLikelyhood(gesName, fv)));
                
//                ghmm.printGesture(gesName);
//                ghmm.printThresholdModel();
//                ghmm.getGestureHmm(gesName).viterbi(fv);
//                System.out.println("Viterbi path : " + ghmm.getGestureHmm(gesName).getViterbiPathString(fv.length));
            }
        }

        /**
         * excutes Guess button
         */
        public void doGuess(){
            String[] fv = fve.convTrajectoryToCodewords(trajectory, -1);
            
            if(fv[0] == null){
                System.out.println("Warning: No trajectory is dected.");
                return;
            }

            String bmg = ghmm.getBestMatchingGesture(fv, fve.vectorAngleSeq);
            gImg.setFont(new Font("Arial", Font.PLAIN, 24));
            
            // erase previous message
            Color tmpColor = getColor();
            gImg.setColor(this.getBackground());
            gImg.drawString(msg, 40 + imgPanelWidth/2 - msg.length()*12/2, imgPanelHeight - 20);
            gImg.setColor(tmpColor);
            
            if(bmg == null){
                msg = "No gesture is found.";
                System.out.println(msg);
            }else{
                msg = String.format("Best matching gesture is %s", bmg);
//                System.out.println(msg +" with probability "+Math.log10(ghmm.getGestureLikelyhood(bmg, fv)));
//                ghmm.getGestureHmm(bmg).viterbi(fv);
//                System.out.println("Viterbi path : " + ghmm.getGestureHmm(bmg).getViterbiPathString(fv.length));
            }
            gImg.drawString(msg, 40 + imgPanelWidth/2 - msg.length()*12/2, imgPanelHeight - 20);
            repaint();

            resetTrajectory();
        }


        /**
         * excutes Show button
         */
        public void doShow(){
            String gesName = (String) gestureChoice.getSelectedItem();
            if(gesName == null || gesName.equals("") || gesName.equals("Select a gesture")){
                System.out.println("Warning: Gesture is not selected.");
                return;
            }

            double[] meanFVarray = ghmm.getAverageFeaturesToArray(gesName);

            float centerX = imgPanelWidth/2;
            float centerY = imgPanelHeight/2;
            float size = Math.min(centerX, centerY);

            clearImage();

            // draws frame
            int margin = 30;
            int shadow = 10;
            Color tmp = getColor();
            gImg.setColor(Color.DARK_GRAY);
            gImg.fillRect((int) (centerX - size/2) - margin + shadow, (int) (centerY - size/2) - margin + shadow, (int) size + 2*margin, (int) size + 2*margin);
            gImg.setColor(Color.WHITE);
            gImg.fillRect((int) (centerX - size/2) - margin, (int) (centerY - size/2) - margin, (int) size + 2*margin, (int) size + 2*margin);
            gImg.setColor(Color.BLACK);
            gImg.drawRect((int) (centerX - size/2) - margin, (int) (centerY - size/2) - margin, (int) size + 2*margin, (int) size + 2*margin);
            gImg.setFont(new Font("Arial", Font.PLAIN, 24));
            gImg.drawString(gesName, (int) centerX - (int) size/2 - margin, (int) centerY - (int) size/2 - margin - 10);
            gImg.setColor(tmp);

            // draws trajectory
            if(ghmm.getGestureHmm(gesName).getNumTraining() > 0)
                drawTrajectory(FeatureExtraction.convAnglesToTrajectoryInScaledArea(new Point2D.Float(centerX, centerY), size, meanFVarray));
            else
                gImg.drawString("Hey, man.", (int) centerX - 50, (int) centerY);

            repaint();
        }

        /**
         * excutes Reset button
         */
        public void doReset(){
            String gesName = (String) gestureChoice.getSelectedItem();
            if(gesName == null || gesName.equals("") || gesName.equals("Select a gesture")){
                System.out.println("Warning: Gesture is not selected.");
                return;
            }

            ghmm.resetGesture(gesName);
            System.out.println(gesName + " is reset now.");
        }


        /**
         * processes Choice events
         * @param e
         */
        public void itemStateChanged(ItemEvent e) {
            if(String.valueOf(e.getSource()).contains("gestureChoice")){
                if(e.getStateChange() == ItemEvent.SELECTED && !String.valueOf(e.getItem()).equals("Select a gesture")){
                    System.out.println("Gesture selection : " + e.getItem() + " is selected.");
                }
            } else {
                if(e.getStateChange() == ItemEvent.SELECTED && !String.valueOf(e.getItem()).equals("Select HMM model")){
                    System.out.println("HMM model selection: " + e.getItem() + " is selected.");
                }
            }
        }
    }

    /**
     * test partial gesture
     */
    public static void testGesture3()
    {
        String[] featureVectorSpace = new String[] {"0", "1", "2", "3", "4", "5"};
        int numState = 4;

        String[][] trainingGesture = {{"0", "1", "2", "3", "4", "5"}, {"3", "2", "1", "0", "4", "5"}, {"5", "4", "3", "2", "1", "0"}};
        String[][] testGesture = {{"0"}, {"0", "1"}, {"0", "1", "2"}, {"0", "1", "2", "3"}, {"0", "1", "2", "3", "4"},
                                  {"0", "1", "2", "3", "4", "5"}, {"0", "1", "2", "3", "4", "5", "1"},
                                  {"0", "1", "2", "3", "4", "5", "1", "3"}, {"0", "1", "2", "3", "4", "5", "1", "3", "5"}};
        String[] TGnames = {"gesture1", "gesture2", "gesture3"};

        GestureHmm ghmm = new GestureHmm(featureVectorSpace, GestureHmm.DYNAMIC_THRESHOLD, 0); // use threshold model
        for(int i=0; i<TGnames.length; i++){
            ghmm.addGesture(TGnames[i], numState, HiddenMarkovModel.ModelType.LRB_RANDOM);
            ghmm.initializeGestureRandom(TGnames[i]);

            if(ghmm.learnGesture(TGnames[i], trainingGesture[i], true, true, true)){
                System.out.println(TGnames[i]+" is properly registered. Log{P(O|model)} = " + Math.log10(ghmm.getGestureHmm(TGnames[i]).forward(trainingGesture[i])));
                ghmm.printGesture(TGnames[i]);
                ghmm.getGestureHmm(TGnames[i]).viterbi(trainingGesture[i]);
                System.out.println("Viterbi path : " + ghmm.getGestureHmm(TGnames[i]).getViterbiPathString(trainingGesture[i].length));
            }
        }

        for(int i=0; i<testGesture.length; i++){
            String bmg = ghmm.getBestMatchingGesture(testGesture[i]);
            System.out.println("Best matching gesture of test gesture " + (i+1) + " is the " + ghmm.getBestMatchingGesture(testGesture[i]) +". ");
            for(int j=0; j<TGnames.length; j++){
                System.out.println("Likelyhood of test gesture " + (i+1) + " in " +TGnames[j]+" : "+ Math.log10(ghmm.getGestureLikelyhood(TGnames[j], testGesture[i])));
            }
            System.out.println("Likelyhood of test gesture " + (i+1) + " in threshold model : "+ Math.log10(ghmm.getGestureLikelyhoodTM(1.0, testGesture[i])));
        }
    }

    /**
     * test gesture recognition based on dynamic threshold model
     */
    public static void testGesture2()
    {
        String[] featureVectorSpace = new String[] {"0", "1", "2", "3"};
        int numState = 4;

        String[][] gesture = {{"0", "1", "2", "3"}, {"1", "3", "2", "1"}, {"3", "1", "1", "2"}, {"2", "1", "0", "0"}};
        String[] names = {"gesture1", "gesture2", "gesture3", "gesture4"};

        GestureHmm ghmm = new GestureHmm(featureVectorSpace, GestureHmm.DYNAMIC_THRESHOLD, 0); // use threshold model
        for(int i=0; i<names.length; i++){
            ghmm.addGesture(names[i], numState, HiddenMarkovModel.ModelType.LR_RANDOM);
            ghmm.initializeGestureRandom(names[i]);

            if(ghmm.learnGesture(names[i], gesture[i], true, true, true)){
                System.out.println(names[i]+" is properly registered. Log{P(O|model)} = " + Math.log10(ghmm.getGestureHmm(names[i]).forward(gesture[i])));
                ghmm.printGesture(names[i]);
                ghmm.getGestureHmm(names[i]).viterbi(gesture[i]);
                System.out.println("Viterbi path : " + ghmm.getGestureHmm(names[i]).getViterbiPathString(gesture[i].length));
            }
        }

        System.out.println(ghmm.getNumGestures()+" guestures are registered.");

        ghmm.printThresholdModel();

        for(int i=0; i<gesture.length; i++){
            System.out.println("Best matching gesture of " + names[i] + " is the " + ghmm.getBestMatchingGesture(gesture[i]) +". ");
            for(int j=0; j<gesture.length; j++){
                System.out.println("    Likelyhood of " + names[j] + " in " + names[i] + " model : "+ Math.log10(ghmm.getGestureLikelyhood(names[i], gesture[j])));
                System.out.println("    Likelyhood of " + names[j] + " in threshold model : "+ Math.log10(ghmm.getGestureLikelyhoodTM(1.0, gesture[j])));
            }
        }
    }


    /**
     * Test dynamic threshold model
     */
    public static void testDynamicThreshold()
    {
        String[] featureVectorSpace = new String[] {"0", "1", "2", "3"};
        int numState = 4;

        String[] gesture1 = new String[] {"0", "1", "2", "3"};
        String name = "gesture1";

        String[] gesture2 = new String[] {"0", "3", "2", "1"};

        GestureHmm ghmm = new GestureHmm(featureVectorSpace, GestureHmm.DYNAMIC_THRESHOLD, 0);
        ghmm.addGesture(name, numState, HiddenMarkovModel.ModelType.LR_RANDOM);
        ghmm.initializeGestureRandom(name);

        if(ghmm.learnGesture(name, gesture1, true, true, true)){
            System.out.println(name+" is properly registered. Log{P(O|model)} = " + Math.log10(ghmm.getGestureHmm(name).forward(gesture1)));
            ghmm.printGesture(name);
            ghmm.getGestureHmm(name).viterbi(gesture1);
            System.out.println(ghmm.getGestureHmm(name).getViterbiPathString(gesture1.length));
        }

        System.out.println(ghmm.getNumGestures()+" guestures are registered.");

        ghmm.printGesture(name);
        ghmm.printThresholdModel();

        System.out.println("Likelyhood of gesture1 in gesture model : "+ Math.log10(ghmm.getGestureLikelyhood(name, gesture1)));
        System.out.println("Likelyhood of gesture1 in threshold model : "+ Math.log10(ghmm.getGestureLikelyhoodTM(0.1, gesture1)));
        System.out.println("Likelyhood of gesture2 in gesture model : "+ Math.log10(ghmm.getGestureLikelyhood(name, gesture2)));
        System.out.println("Likelyhood of gesture2 in threshold model : "+ Math.log10(ghmm.getGestureLikelyhoodTM(0.1, gesture2)));
    }

    /**
     * Test the performance of gesture recognition module. All possible observations are scanned.
     */
    public static void testGesture1()
    {
        String[] featureVectorSpace = new String[] {"0", "1", "2", "3"};
        String[][] observations = GestureHmm.genCompleteObsSeqSet(featureVectorSpace, 4, false);
        int numState = 4;

        String[] gesture1 = new String[] {"0", "1", "2", "3"};
        String name = "gesture1";
        
        GestureHmm ghmm = new GestureHmm(featureVectorSpace, GestureHmm.NO_THRESHOLD, 0);
        ghmm.addGesture(name, numState, HiddenMarkovModel.ModelType.LR_RANDOM);
        ghmm.initializeGestureRandom(name);

        if(ghmm.learnGesture(name, gesture1, true, true, true)){
            System.out.println(name+" is properly registered. Log{P(O|model)} = " + Math.log10(ghmm.getGestureHmm(name).forward(gesture1)));
            ghmm.printGesture(name);
            ghmm.getGestureHmm(name).viterbi(gesture1);
            System.out.println(ghmm.getGestureHmm(name).getViterbiPathString(gesture1.length));
        }

        System.out.println(ghmm.getNumGestures()+" guestures are registered.");

        for(String[] obs : observations){
            double dis = calDistance(gesture1, obs, 2);
            System.out.println(dis + ", "+ Math.log10(ghmm.getGestureLikelyhood(name, obs)));
        }
    }

    /**
     *
     * @param obs1
     * @param obs2
     * @param maxDiff
     * @return
     */
    public static double calDistance(String[]obs1, String[] obs2, int maxDiff){
        double sum = 0;

        for(int i=0; i<Math.min(obs1.length, obs2.length); i++){
            int diff = Math.abs(Integer.parseInt(obs1[i]) - Integer.parseInt(obs2[i]));
            if(diff > maxDiff){
                if(maxDiff%2 == 0)
                    diff = 2*maxDiff - diff;
                else
                    diff = 2*maxDiff + 1 - diff;
            }
            sum += Math.pow(diff, 2.0);
        }

        return Math.sqrt(sum)/obs1.length;
    }

    /**
     * 
     * @param obs1
     * @param obs2
     * @return
     */
    public static double calDistance2(String[]obs1, String[] obs2){
        double sum = 0;

        for(int i=0; i<Math.min(obs1.length, obs2.length); i++){
            if(!obs1[i].equals(obs2[i]))
                sum += 1.0;
        }

        return sum/obs1.length;
    }

    /**
     * Test forward, backward, and Viterbi algorithm with silent states
     */
    public static void testSilentState()
    {
        String[] observations1 = new String[] {"o1", "o3", "o3", "o1", "o2"};
        String[] observations2 = new String[] {"o3", "o1", "o2", "o1", "o1", "o2", "o1", "o1", "o3"};

        String[] observationSet = new String[] {"o1", "o2", "o3"};

        String[] states1 = new String[] {"start", "a1", "a2", "final"};
        double[] startProbability1 = {1, 0, 0, 0};
        double[][] transitionProbability1 = {{0, 1.0, 0, 0}, {0, 0.3, 0.7, 0}, {0, 0, 0.5, 0.5}, {1.0, 0, 0, 0}};
        double [][] emissionProbability1 = {{0, 0, 0}, {0.1, 0.2, 0.7}, {0.3, 0.5, 0.2}, {0, 0, 0}};

        String[] states2 = new String[] {"a1", "a2"};
        double[] startProbability2 = {1, 0};
        double[][] transitionProbability2 = {{0.3, 0.7}, {0.5, 0.5}};
        double [][] emissionProbability2 = {{0.1, 0.2, 0.7}, {0.3, 0.5, 0.2}};

        HiddenMarkovModel hmm1 = new HiddenMarkovModel("example3_1", states1, observationSet, HiddenMarkovModel.ModelType.USER_DEFINED);
        hmm1.setStartProbability(startProbability1);
        hmm1.setTransitionProbability(transitionProbability1);
        hmm1.setEmissionProbability(emissionProbability1);
        hmm1.printAllProbability();

        HiddenMarkovModel hmm2 = new HiddenMarkovModel("example3_2", states2, observationSet, HiddenMarkovModel.ModelType.USER_DEFINED);
        hmm2.setStartProbability(startProbability2);
        hmm2.setTransitionProbability(transitionProbability2);
        hmm2.setEmissionProbability(emissionProbability2);
        hmm2.printAllProbability();

        Object[] objs;
        System.out.println("Forward likelyhood of observation1 in " + hmm1.getName() + " : " + hmm1.forward(observations1));
        System.out.println("Forward likelyhood of observation1 in " + hmm2.getName() + " : " + hmm2.forward(observations1));
        System.out.println("Backward Likelyhood of observation1 in " + hmm1.getName() + " : " + hmm1.backward(observations1));
        System.out.println("Backward Likelyhood of observation1 in " + hmm2.getName() + " : " + hmm2.backward(observations1));
        objs = hmm1.viterbi(observations1);
        System.out.println("Viterbi of observation1 in " + hmm1.getName() + " : v_path = " + hmm1.getViterbiPathString(observations1.length) + ", v_prob = " + (Double) objs[2]);
        objs = hmm2.viterbi(observations1);
        System.out.println("Viterbi of observation1 in " + hmm2.getName() + " : v_path = " + hmm2.getViterbiPathString(observations1.length) + ", v_prob = " + (Double) objs[2]);

        System.out.println("Forward likelyhood of observation2 in " + hmm1.getName() + " : " + hmm1.forward(observations2));
        System.out.println("Forward likelyhood of observation2 in " + hmm2.getName() + " : " + hmm2.forward(observations2));
        System.out.println("Backward Likelyhood of observation2 in " + hmm1.getName() + " : " + hmm1.backward(observations2));
        System.out.println("Backward Likelyhood of observation2 in " + hmm2.getName() + " : " + hmm2.backward(observations2));
        objs = hmm1.viterbi(observations2);
        System.out.println("Viterbi of observation2 in " + hmm1.getName() + " : v_path = " + hmm1.getViterbiPathString(observations2.length) + ", v_prob = " + (Double) objs[2]);
        objs = hmm2.viterbi(observations2);
        System.out.println("Viterbi of observation2 in " + hmm2.getName() + " : v_path = " + hmm2.getViterbiPathString(observations2.length) + ", v_prob = " + (Double) objs[2]);

    }

    /**
     * Test Baum-Welch learning
     */
    public static void testBaumWelch()
    {
        String[] observations = new String[] {"o1", "o2", "o3"};
        String[] observations1 = new String[] {"o2", "o1", "o3"};
        String[] observations2 = new String[] {"o3", "o2", "o1"};

        String[] states = new String[] {"a1", "a2", "a3"};
        String[] observationSet = new String[] {"o1", "o2", "o3"};
        double[] startProbability = {1, 0, 0, 0};
        Object[][] transitionProbability = new Object[][] {{"a1", "a1", 0.3}, {"a1", "a2", 0.7}, {"a2", "a2", 0.3}, {"a2", "a3", 0.7}, {"a3", "a3", 0.3}, {"a3", "a1", 0.7}};
        double [][] emissionProbability = {{0.7, 0.2, 0.1}, {0.2, 0.6, 0.2}, {0.1, 0.2, 0.7}};

        HiddenMarkovModel hmm = new HiddenMarkovModel("example2", states, observationSet, HiddenMarkovModel.ModelType.USER_DEFINED);
        hmm.setStartProbability(startProbability);
        for(Object[] ob: transitionProbability)
            hmm.setTransitionProbability((String) ob[0], (String) ob[1], ((Double) ob[2]).doubleValue());
        hmm.setEmissionProbability(emissionProbability);


        System.out.println("Probability before learning = " + hmm.forward(observations));
        Object[] ret = hmm.viterbi(observations);
        System.out.println("The best path for observation (" + observations[0]+", " + observations[1]+", " + observations[2]+") is "+hmm.getViterbiPathString(observations.length)+" with probability "+((Double) ret[2]).floatValue());
//        hmm.printAllProbability();
        hmm.BaumWelch(observations, 0.00001, 0.0001, true, true, true, true);
        System.out.println("Probability after learning = " + hmm.forward(observations));
        hmm.printAllProbability();
        ret = hmm.viterbi(observations);
        System.out.println("The best path for observation (" + observations[0]+", " + observations[1]+", " + observations[2]+") is "+hmm.getViterbiPathString(observations.length)+" with probability "+((Double) ret[2]).floatValue());
        ret = hmm.viterbi(observations1);
        System.out.println("The best path for observation (" + observations1[0]+", " + observations1[1]+", " + observations1[2]+") is "+hmm.getViterbiPathString(observations1.length)+" with probability "+((Double) ret[2]).floatValue());
        ret = hmm.viterbi(observations2);
        System.out.println("The best path for observation (" + observations2[0]+", " + observations2[1]+", " + observations2[2]+") is "+hmm.getViterbiPathString(observations2.length)+" with probability "+((Double) ret[2]).floatValue());
        
    }

    /**
     * Test Viterbi algorithm
     */
    public static void testViterbi()
    {
        String[][] observations = new String[][] {{"o1", "o2", "o3"}, {"o2", "o3", "o2"}, {"o1", "o1", "o2"}, {"o3", "o1", "o2"}, {"o2", "o1", "o3"}, {"o3", "o3", "o3"}};

        String[] states = new String[] {"a1", "a2", "a3", "a4", "b1", "b2", "b3", "b4"};
        String[] observationSet = new String[] {"o1", "o2", "o3"};
        double[] startProbability = {0.5, 0, 0, 0, 0.5, 0, 0, 0};
        Object[][] transitionProbability = new Object[][] {{"a1", "a1", 0.3}, {"a1", "a2", 0.7}, {"a2", "a2", 0.3}, {"a2", "a3", 0.7}, {"a3", "a4", 1.0},
                                                            {"b1", "b1", 0.3}, {"b1", "b2", 0.7}, {"b2", "b2", 0.3}, {"b2", "b3", 0.7}, {"b3", "b4", 1.0}};
        double [][] emissionProbability = {{0.7, 0.2, 0.1}, {0.2, 0.6, 0.2}, {0.1, 0.2, 0.7}, {0, 0, 0},
                                           {0.1, 0.8, 0.1}, {0.1, 0.2, 0.7}, {0.2, 0.6, 0.2}, {0, 0, 0}};

        HiddenMarkovModel hmm = new HiddenMarkovModel("example1", states, observationSet, HiddenMarkovModel.ModelType.USER_DEFINED);

        // start probability
        hmm.setStartProbability(startProbability);


        // transition_probability
        for(Object[] ob: transitionProbability)
            hmm.setTransitionProbability((String) ob[0], (String) ob[1], ((Double) ob[2]).doubleValue());

        // emission_probability
        hmm.setEmissionProbability(emissionProbability);

        for(int k= 0; k < observations.length; k++){
            Object[] ret = hmm.viterbi(observations[k]);
            System.out.println("The best path for observation (" + observations[k][0]+", " + observations[k][1]+", " + observations[k][2]+") is "+hmm.getViterbiPathString(observations[k].length)+" with probability "+((Double) ret[2]).floatValue());
        }
    }

    /**
     * Bob and Alice in Wikipedia
     */
    public static void bob_Alice()
    {
        String[] observations = new String[] {"walk","shop","clean"};

        String[] states = new String[] {"Rainy","Sunny"};
        double [] startProbability = {0.6, 0.4};
        String[] observationSet = new String[] {"walk","shop","clean"};

        HiddenMarkovModel hmm = new HiddenMarkovModel("Bob and Alice", states, observationSet, HiddenMarkovModel.ModelType.USER_DEFINED);

        // start probability
        hmm.setStartProbability(startProbability);

        // transition_probability
        hmm.setTransitionProbability("Rainy", "Rainy", 0.7);
        hmm.setTransitionProbability("Rainy", "Sunny", 0.3);
        hmm.setTransitionProbability("Sunny", "Rainy", 0.4);
        hmm.setTransitionProbability("Sunny", "Sunny", 0.6);

        // emission_probability
        hmm.setEmissionProbability("Rainy", "walk", 0.1);
        hmm.setEmissionProbability("Rainy", "shop", 0.4);
        hmm.setEmissionProbability("Rainy", "clean", 0.5);

        hmm.setEmissionProbability("Sunny", "walk", 0.6);
        hmm.setEmissionProbability("Sunny", "shop", 0.3);
        hmm.setEmissionProbability("Sunny", "clean", 0.1);

        Object[] ret = hmm.viterbi(observations);

        System.out.println("Alice guesses that the weather was "+ hmm.getViterbiPathString(observations.length) +" with probability "+((Double) ret[2]));

        for(String st: states){
           System.out.println(st+" : v_path="+hmm.getViterbiPathString(observations.length, st)+", v_prob="+hmm.getViterbiPathProbability(observations.length, st));
        }
    }
}

