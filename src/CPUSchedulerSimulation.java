//todo: ask question about finishing time, check with printTurnAroundTime.
import java.io.*;
import java.util.*;

public class CPUSchedulerSimulation {

    public static void main(String[] args) throws FileNotFoundException {
        int nbrOfCPU = 0;
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
        cs.executeFCFSMulti(nbrOfCPU);
//        CPUScheduler.timeClock = 0;
        //cs.executeSJF();
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
    int finishingTime;
    int firstClockTimeItIsRunning;

    int cpuCore;

    public PCB(int arrivalTime) {
        this.processState = ProcessState.NEW;
        this.programCounter = 0; //PC starts at 0 when PCB is created
        this.clockTimeSinceIORequest = 0; //when process gets an IOrequest, we need to track clock time up to 5
        this.arrivalTime = arrivalTime;
        this.cpuCore = 0;
        this.finishingTime = 0;
        this.firstClockTimeItIsRunning = -1;
    }


    public PCB(PCB p){
        this.processState = p.processState;
        this.programCounter = p.programCounter;
        this.clockTimeSinceIORequest = p.clockTimeSinceIORequest;
        this.arrivalTime = p.arrivalTime;
        this.cpuCore = 0;
        this.finishingTime = 0;
        this.firstClockTimeItIsRunning = -1;
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
                ", finishingTime=" + finishingTime +
                ", firstClockTimeItIsRunning=" + firstClockTimeItIsRunning +
                ", cpuCore=" + cpuCore +
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
        process.pcb.cpuCore = 0;
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
    public void increaseCounter(ArrayList<Process> currentProcesses){
        for (Process process : currentProcesses) {
            process.pcb.programCounter++;
        }
    }


    public void executeFCFSMulti(int nbrOfCPU){
        System.out.println("Executing FCFS on " + nbrOfCPU + " cores");
        if(nbrOfCPU == 0){
            System.out.println("CPU has 0 cores, impossible to run..");
            return;
        }
        ArrayList<Process> currentProcesses = new ArrayList<>();
        boolean[] isFreeCore = new boolean[nbrOfCPU];
        Arrays.fill(isFreeCore, true);

        while (!isAllProcessTerminated()){
            addArrivals(timeClock);
            int nbrOfFreeCpu = nbrOfCPU - currentProcesses.size();

            //add to currentProcesses
            while (nbrOfFreeCpu != 0 && !readyQueue.isEmpty()){
                for (int i = 0; i < isFreeCore.length; i++) {
                    if (isFreeCore[i]){
                        Process p = readyQueue.poll();
                        p.pcb.cpuCore = i+1;
                        p.pcb.processState = ProcessState.RUNNING;
                        if(p.pcb.firstClockTimeItIsRunning == -1){
                            p.pcb.firstClockTimeItIsRunning = timeClock;
                        }
                        currentProcesses.add(p);
                        isFreeCore[i] = false;
                        break;
                    }
                }
                nbrOfFreeCpu = nbrOfCPU - currentProcesses.size();
            }

            //increase counter
            increaseCounter(currentProcesses);
            updateWaitQueueTime();


            printInfo(currentProcesses, nbrOfCPU);

            Iterator<Process> currentProcessIterator = currentProcesses.iterator();

            while (currentProcessIterator.hasNext()){
                Process currentProcess = currentProcessIterator.next();
                //add to waitqueue
                if (currentProcess.ioRequests.contains(currentProcess.pcb.programCounter)) {
                    isFreeCore[currentProcess.pcb.cpuCore-1] = true;
                    addProcessToWaitQueue(currentProcess);
                    currentProcessIterator.remove();
                }
                //terminated.
                if (currentProcess.pcb.programCounter == currentProcess.totalExecTime && !currentProcess.pcb.processState.equals(ProcessState.WAITING)) {
                    isFreeCore[currentProcess.pcb.cpuCore-1] = true;
                    currentProcess.pcb.processState = ProcessState.TERMINATED;
                    currentProcess.pcb.finishingTime = timeClock;
                    currentProcessIterator.remove();
                }
            }

            addWaitingProcessToReadyQueueOrTerminated();
            timeClock++;
        }
        //All processes terminated
        printInfo(currentProcesses, nbrOfCPU);
        System.out.println("--------------");
        System.out.println("Additional Information on FCFS on "+nbrOfCPU+" cores");

        //todo: print CPU utilization
        printCPUUtilization(processes, timeClock, nbrOfCPU);
        //todo: print Avg wait time
        printWaitingTime(processes, timeClock);
        //todo: print turnaround time for each process
        printTurnAroundTime(processes);
        //todo: print CPU response time for each process
        printCPUResponseTime(processes);
    }

    //non preemptive.
    public void executeSJF(){

    }


    public void printInfo(int nbrOfCPU){
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
    public void printInfo(ArrayList<Process> currentProcesses, int nbrOfCPU){
        System.out.println("--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
        System.out.println("Time clock: " + timeClock);

        System.out.println("Running");


        for (int i = 0; i < nbrOfCPU; i++) {
            System.out.println("\tCPU core " + (i+1));
            for (Process process: currentProcesses) {
                if (process.pcb.processState.equals(ProcessState.RUNNING) && process.pcb.cpuCore==i+1) System.out.println("\t\t"+process);
            }
        }


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
    public void printCPUUtilization(ArrayList<Process> processes, int timeClock, int nbrOfCPU){
        int totalProgramCounters = 0;
        for (Process p :
                processes) {
            totalProgramCounters += p.totalExecTime;
        }

        double CPUUtilization = ((double) totalProgramCounters)/((double)(timeClock-1)*(double)nbrOfCPU) * 100;
        String result = String.format("%.2f", CPUUtilization);

        System.out.println("CPU Utilization = " + result + "%");
    }

    public void printWaitingTime(ArrayList<Process> processes,int timeClock){
        ArrayList<Integer> waitingTime = new ArrayList<>();
        for (Process process: processes) {
            int singleWaitTime = process.pcb.finishingTime - process.pcb.arrivalTime - process.totalExecTime;
            waitingTime.add(singleWaitTime);
        }
        double avgWaitingTime=0;
        for (int waitTime : waitingTime) {
            avgWaitingTime += waitTime;
        }
        avgWaitingTime = avgWaitingTime / (double) waitingTime.size();
        String result = String.format("%.2f", avgWaitingTime);

        System.out.println("Average Waiting Time = " + result + " Time Clock") ;
    }
    public void printTurnAroundTime(ArrayList<Process> processes){
        System.out.println("Turnaround Time:");
        for (Process process: processes) {
            int singleTurnAround = process.pcb.finishingTime - process.pcb.arrivalTime;
            System.out.println("\t PID=" + process.PID + " = " + singleTurnAround + " Time Clock");
        }
    }
    public void printCPUResponseTime(ArrayList<Process> processes){
        System.out.println("CPU Response Time:");
        for (Process process: processes) {
            int firstResponseTime = process.pcb.firstClockTimeItIsRunning - process.pcb.arrivalTime;
            System.out.println("\t PID=" + process.PID + " = " + firstResponseTime + " Time Clock");
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