import java.io.*;
import java.util.*;

public class CPUSchedulerSimulation {

    public static void main(String[] args) throws FileNotFoundException {
        int nbrOfCPU;
        int q;
        ArrayList<String> processesDataString = new ArrayList<>();
        ArrayList<Process> processes = new ArrayList<>();

        //Read file
        File file;
        FileOutputStream fos = null;
        PrintStream fileOut = null;
        try {
            file = new File("output.txt");
            fos = new FileOutputStream(file);
            fileOut = new PrintStream(fos);
            System.setOut(fileOut);


            //read input.txt
            File myObj = new File("./src/input.txt");
            Scanner myReader = new Scanner(myObj);

            String line1 = myReader.nextLine();
            String line2 = myReader.nextLine();
            nbrOfCPU =  Character.getNumericValue(line1.charAt(line1.length()-1));  //parse NbrOfCpus
            q =  Character.getNumericValue(line2.charAt(line2.length()-1));  //parse q

            //Skip next 2 lines
            myReader.nextLine();
            myReader.nextLine();

            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                processesDataString.add(data);
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        for (int i = 0; i < processesDataString.size(); i++) {
            processes.add(parseProcessDataString(processesDataString.get(i)));
        }


        CPUScheduler cs = new CPUScheduler(processes);

        //todo: make a different method for each Schedule
        cs.executeFCFS();
//        CPUScheduler.timeClock = 0;
        //cs.executeSJB()
//        CPUScheduler.timeClock = 0;
        //cs.executeRR(2)



        try{
            fileOut.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //TODO: Done
    public static Process parseProcessDataString(String s){
        ArrayList<Integer> ioRequests = new ArrayList<>();

        String[] parts = s.split("\t");
        int PID = Character.getNumericValue(parts[0].charAt(parts[0].length()-1));  //good
        int arrivalTime = Integer.parseInt(parts[1]);
        int totalExecTime = Integer.parseInt(parts[2]);

        String part4 = parts[3].replace("[", "").replace("]", "");
        String[] part4Strings = part4.split(", ");

        for (int i = 0; i < part4Strings.length; i++) {
            String[] numbersString = part4Strings[i].split(",");
            for (int j = 0; j < numbersString.length; j++) {
                if(numbersString[i].equals("")) continue;
                int currentNum = Integer.parseInt(numbersString[j]);
                if (i==0){
                    ioRequests.add(currentNum);
                }
            }
        }

        Process process = new Process(PID, arrivalTime,  ioRequests, totalExecTime);
        return  process;
    }
}

class Process{
    int PID;
    PCB pcb;
    ArrayList<Integer> ioRequests = new ArrayList<>();
    int totalExecTime;

    public Process(int PID, int arrivalTime , ArrayList<Integer> ioRequests, int totalExecTime) {
        this.PID = PID;
        this.totalExecTime = totalExecTime;
        this.pcb = new PCB(arrivalTime);
        this.ioRequests = ioRequests;
    }

    //copy constructor
    public Process(Process p){
        this.PID = p.PID;
        this.pcb = p.pcb.clone();
        this.ioRequests = p.ioRequests;
        this.totalExecTime = p.totalExecTime;
    }

    @Override
    public Process clone(){
        return new Process(this);
    }

    @Override
    public String toString() {
        return "Process{" +
                "PID=" + PID +
                ", pcb=" + pcb +
                ", ioRequests=" + ioRequests +
                ", totalExecTime=" + totalExecTime +
                '}';
    }


}
class PCB{
    ProcessState processState;
    int programCounter;
    int clockTimeSinceIORequest;
    int arrivalTime;

    public PCB(int arrivalTime) {
        this.processState = ProcessState.NEW;
        this.programCounter = 0; //PC starts at 0 when PCB is created
        this.clockTimeSinceIORequest = 0; //when process gets an IOrequest, we need to track clock time up to 5
        this.arrivalTime = arrivalTime;
    }


    public PCB(PCB p){
        this.processState = p.processState;
        this.programCounter = p.programCounter;
        this.clockTimeSinceIORequest = p.clockTimeSinceIORequest;
        this.arrivalTime = p.arrivalTime;
    }

    @Override
    public PCB clone(){
        return new PCB(this);
    }

    @Override
    public String toString() {
        return "PCB{" +
                "processState=" + processState +
                ", programCounter=" + programCounter +
                ", clockTimeSinceIORequest=" + clockTimeSinceIORequest +
                ", arrivalTime=" + arrivalTime +
                '}';
    }
}

class CPUScheduler {

    static int timeClock = 0;
    Queue<Process> readyQueue = new LinkedList<>();
    Queue<Process> waitQueue = new LinkedList<>();
    ArrayList<Process> processes = new ArrayList<>();

    //start by adding a process to the ready queue.
    public CPUScheduler(ArrayList<Process> processes) {
        this.processes = processes;
    }

    public void addProcessToReadyQueue(Process process){
        process.pcb.processState = ProcessState.READY;
        readyQueue.add(process);
    }

    public void addProcessToWaitQueue(Process process){
        process.pcb.processState = ProcessState.WAITING;
        waitQueue.add(process);
    }

    //Check the first element of the  waitqueue, add +1 to clockTimeSinceIORequest
    public void updateWaitQueueTime(){
        Process waitingProcess = waitQueue.peek();

        if (waitingProcess != null
                && waitingProcess.pcb.processState.equals(ProcessState.WAITING)
                && waitingProcess.pcb.clockTimeSinceIORequest < 2) {
            waitingProcess.pcb.clockTimeSinceIORequest++;
        }
    }

    public void addWaitingProcessToReadyQueueOrTerminated(){
        Process waitingProcess = waitQueue.peek();
        if (waitingProcess != null
                && waitingProcess.pcb.processState.equals(ProcessState.WAITING)
                && waitingProcess.pcb.clockTimeSinceIORequest == 2) {

            //terminated
            if (waitingProcess.totalExecTime == waitingProcess.pcb.programCounter){
                waitingProcess.pcb.processState = ProcessState.TERMINATED;
                waitingProcess.pcb.clockTimeSinceIORequest = 0;
                waitQueue.poll();

            }else{ //goes to ready queue
                waitingProcess.pcb.processState = ProcessState.READY;
                waitingProcess.pcb.clockTimeSinceIORequest = 0;
                waitQueue.poll();
                readyQueue.add(waitingProcess);
            }
        }
    }

    public void addArrivals(int timeClock){
        for (Process process : processes) {
            if (process.pcb.arrivalTime == timeClock) addProcessToReadyQueue(process);
        }
    }
    public boolean isAllProcessTerminated(){
        for (Process process : processes) {
            if (!process.pcb.processState.equals(ProcessState.TERMINATED)) return false;
        }
        return true;
    }

    public void executeFCFS(){
        Process currentProcess = null;
        boolean isTerminated = false;
        boolean isWaitQueue = false;

        while (!isAllProcessTerminated()) {
            addArrivals(timeClock);
            if (!readyQueue.isEmpty() || currentProcess != null){
                if(currentProcess == null) currentProcess = readyQueue.poll();
                currentProcess.pcb.processState = ProcessState.RUNNING;
                currentProcess.pcb.programCounter++;
                updateWaitQueueTime();


                printInfo(currentProcess);
                //add to waitqueue
                if (currentProcess.ioRequests.contains(currentProcess.pcb.programCounter)) {
//                    printInfo(currentProcess);
                    addProcessToWaitQueue(currentProcess);
                    currentProcess = null;
                    timeClock++;
                    continue;
                }

                //Last instruction, so set it as terminated.
                if (currentProcess.pcb.programCounter == currentProcess.totalExecTime && !currentProcess.pcb.processState.equals(ProcessState.WAITING)) {
//                    printInfo(currentProcess);
                    currentProcess.pcb.processState = ProcessState.TERMINATED;
                    currentProcess = null;
                    timeClock++;
                    continue;
                }
                addWaitingProcessToReadyQueueOrTerminated();



            }else {
                updateWaitQueueTime();
                printInfo();
                addWaitingProcessToReadyQueueOrTerminated();
            }
            timeClock++;
        }
        //print all terminated stuff
        printInfo();




//        Process currentProcess = null;
//        boolean resetCurrentProcess = false;
//
//        while(!readyQueue.isEmpty() || !waitQueue.isEmpty()){
//            if(currentProcess != null && currentProcess.pcb.processState.equals(ProcessState.RUNNING)){
//                //run
//                currentProcess.pcb.programCounter++;
//                updateWaitQueueTime();
//                printInfo(currentProcess);
//
//                //goes in waitqueue
//                if (currentProcess.ioRequests.contains(currentProcess.pcb.programCounter)){
//                    waitQueue.add(currentProcess.clone());
//                    currentProcess = null;
//                }
////                if (currentProcess)
//
//                timeClock++;
//                printInfo(currentProcess);
//                addWaitingProcessToReadyQueueOrTerminated();
//            }else{
//                if (readyQueue.isEmpty()){
//                    updateWaitQueueTime();
//                }else{
//                    currentProcess = readyQueue.poll();
//                }
//            }
//        }

        /*
        1.while readyQueue and waitQueue is not empty
            2. Check if we have currentProcess
            3. if currentProcess running then just run it
            4. if currentProcess is not running then
                5. check if readyQueue is empty
                    if ready queue empty, then just update the waitqueue
                    if readyqueue is not empty, get the process and run it

         * */

//
//        while (!readyQueue.isEmpty() || !waitQueue.isEmpty()) {
//
//            //get a new process
//            if (currentProcess == null){
//                currentProcess = readyQueue.poll();
//            }
//            //if currentProcess IS STILL NULL (readyqueue empty)
//            if (currentProcess == null){
//                updateWaitQueueTime();
//                printInfo();
//                addWaitingProcessToReadyQueueOrTerminated();
//            }else{
//                currentProcess.pcb.processState = ProcessState.RUNNING;
//                currentProcess.pcb.programCounter++;
//                updateWaitQueueTime();
//
//                //currentProcess goes to Waitqueue
//                if (currentProcess.ioRequests.contains(currentProcess.pcb.programCounter)) {
//                    printInfo(currentProcess);
//                    addProcessToWaitQueue(currentProcess.clone()); //adding process to wait queue
//                    resetCurrentProcess = true;
//                }
//
//                //Last instruction, so set it as terminated.
//                if (currentProcess.pcb.programCounter == currentProcess.totalExecTime) {
//                    printInfo(currentProcess);
//                    currentProcess.pcb.processState = ProcessState.TERMINATED;
//                    processes.remove(currentProcess);
//                    processes.add(currentProcess.clone());
//                    resetCurrentProcess = true;
//                }
//
//                printInfo(currentProcess);
//
//                //Check in the waitQueue if there's process ready to be added to ready queue.
//                addWaitingProcessToReadyQueueOrTerminated();
//
//                if (currentProcess.pcb.processState.equals(ProcessState.RUNNING)){
//                    currentProcess.pcb.processState = ProcessState.READY;
//                    readyQueue.add(currentProcess.clone());
//                }
//            }
//        }
//        //print all terminated stuff
//        printInfo();
    }

    public void printInfo(){
        System.out.println("--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
        System.out.println("Time clock: " + timeClock);
        System.out.println("Running");
        System.out.println("Ready Queue");

        for (Process process : readyQueue) {
            System.out.println("\t"+process);
        }

        System.out.println("Wait Queue");
        for (Process process : waitQueue) {
            System.out.println("\t"+process);
        }

        System.out.println("Terminated");
        for (Process process : processes) {
            if (process.pcb.processState.equals(ProcessState.TERMINATED)){
                System.out.println("\t" + process);
            }
        }
    }
    public void printInfo(Process currentProcess){
        System.out.println("--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
        System.out.println("Time clock: " + timeClock);

        System.out.println("Running");
        if (currentProcess.pcb.processState.equals(ProcessState.RUNNING)) System.out.println("\t"+currentProcess);
        System.out.println("Ready Queue");

        for (Process process : readyQueue) {
            System.out.println("\t"+process);
        }
        System.out.println("Wait Queue");
        for (Process process : waitQueue) {
            System.out.println("\t"+process);
        }

        System.out.println("Terminated");
        for (Process process : processes) {
            if (process.pcb.processState.equals(ProcessState.TERMINATED)){
                System.out.println("\t" + process);
            }
        }

    }


}



enum ProcessState {
    NEW,
    RUNNING,
    WAITING,
    READY,
    TERMINATED
}