/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation
 *               of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009, The University of Melbourne, Australia
 */


package org.cloudbus.cloudsim.examples;

import java.lang.invoke.MethodHandles;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.Random;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

public class RALBA2 {
	// Has better to source code ogradl, but seemed to make no difference

	private static int WORKLOAD = 1;
	// 0 Synthetic
	// 1 Google like
	// 2 Synthetic + Google Like
	
	private static int SCHEDULER = 3;
	// 0 RS
	// 1 RR
	// 2 MCT
	// 3 RALBA
	// 4 OGRADL
	
	private static int ALLOW_REJECTS_TO_RUN = 0;
	// 0 Will not let rejected task run
	// 1 Will let rejected task run
	
	private static double DEADLINE_MIN = 100.0;
	private static double DEADLINE_MAX = 400.0;
	
	private static double DEADLINE_GOOGLE_MIN = 100.0;
	private static double DEADLINE_GOOGLE_MAX = 400.0;
	private static double DEADLINE_SYN_MIN = 2.0;
	private static double DEADLINE_SYN_MAX = 8.0;
	
	public static int t_count = 100;
	public static Double[] t_makespan = new Double[t_count];
	public static Double[] t_throughput = new Double[t_count];
	public static Double[] t_arur = new Double[t_count];
	public static Double[] t_accept = new Double[t_count];
	public static Double[] t_reject = new Double[t_count];
	
	public static int genRandom(int low, int high) {
		return new Random().nextInt(high + 1 - low) + low;
	}

	public static Double genRandomDouble(int low, int high) {
		return low + (high - low) * new Random().nextDouble();
	}

	public static Double genRandomDouble(double low, double high) {
		return low + (high - low) * new Random().nextDouble();
	}
	
	private static List<Vm> createVMAdvance(int userId, int vms, int mipslow, int mipshigh) {

		//Creates a container to store VMs. This list is passed to the broker later
		LinkedList<Vm> list = new LinkedList<Vm>();

		//VM Parameters
		long size = 10000; //image size (MB)
		int ram = 512; //vm memory (MB)
		int mips = RALBA2.genRandom(mipslow, mipshigh);
		long bw = 1000;
		int pesNumber = 1; //number of cpus
		String vmm = "Xen"; //VMM name

		//create VMs
		Vm[] vm = new Vm[vms];

		for(int i=0;i<vms;i++){
			vm[i] = new Vm(i, userId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
			//for creating a VM with a space shared scheduling policy for cloudlets:
			//vm[i] = Vm(i, userId, mips, pesNumber, ram, bw, size, priority, vmm, new CloudletSchedulerSpaceShared());

			list.add(vm[i]);
		}

		return list;
	}

	private static List<Vm> createVMAdvance(int userId, int vms, int mips) {
		return createVMAdvance(userId, vms, mips, mips);
	}
	
	private static List<Vm> createVM(int userId, int vms) {
		return createVMAdvance(userId, vms, 800, 1100);
	}
	
	private static List<Vm> createExperimentVM(int userId) {

		//Creates a container to store VMs. This list is passed to the broker later
		LinkedList<Vm> list = new LinkedList<Vm>();
		
		LinkedList<Integer> vmc = new LinkedList<>();
		int ll[] = {7, 6,6,6,6,6,6};
		int lz[] = {500,750,1000,1250,1500,1750,4000};
		
		for(int i = 0; i < ll.length; i++)
		{
			for(int j = 0; j < ll[i]; j++) {
				vmc.add(lz[i]);
			}
		}

		//VM Parameters
		long size = 10000; //image size (MB)
		int ram = 512; //vm memory (MB)
		long bw = 1000;
		int pesNumber = 1; //number of cpus
		String vmm = "Xen"; //VMM name

		//create VMs
		Vm[] vm = new Vm[vmc.size()];

		for(int i=0;i<vmc.size();i++){
			vm[i] = new Vm(i, userId, vmc.get(i), pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
			//for creating a VM with a space shared scheduling policy for cloudlets:
			//vm[i] = Vm(i, userId, mips, pesNumber, ram, bw, size, priority, vmm, new CloudletSchedulerSpaceShared());

			list.add(vm[i]);
		}

		return list;
	}


	private static List<Cloudlet> createCloudletAdvance(int userId, int cloudlets, int milow, int mihigh){
		// Creates a container to store Cloudlets
		LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();

		//cloudlet parameters
		long fileSize = 300;
		long outputSize = 300;
		int pesNumber = 1;
		UtilizationModel utilizationModel = new UtilizationModelFull();

		Cloudlet[] cloudlet = new Cloudlet[cloudlets];

		for(int i=0;i<cloudlets;i++){
			long length = milow + (i * ( (mihigh - milow) / (cloudlets - 1) ) );
			cloudlet[i] = new Cloudlet(i, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
			// setting the owner of these Cloudlets
			cloudlet[i].setUserId(userId);
			list.add(cloudlet[i]);
		}

		return list;
	}
	
	private static List<Cloudlet> createCloudletAdvance(int userId, int cloudlets, int mi){
		return createCloudletAdvance(userId, cloudlets, mi, mi);
	}
	
	
	private static List<Cloudlet> createCloudlet(int userId, int cloudlets){
		return createCloudletAdvance(userId, cloudlets, 900, 1000);
	}
	
	private static List<Cloudlet> createSyntheticCloudlet(int userId){
		
		LinkedList<Integer> clc = new LinkedList<>();
		int ll[] = {60, 5, 10, 5};
		int lz[][] = {{800,1200}, {1800,2500}, {7000, 10000}, {30000, 45000}};
		
		for(int i = 0; i < ll.length; i++)
		{
			for(int j = 0; j < ll[i]; j++) {
				int milow = lz[i][0];
				int mihigh = lz[i][1];
				clc.add(milow + (j * ( (mihigh - milow) / (ll[i] - 1) ) ));
			}
		}
		
		// Creates a container to store Cloudlets
		LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();

		//cloudlet parameters
		long fileSize = 300;
		long outputSize = 300;
		int pesNumber = 1;
		UtilizationModel utilizationModel = new UtilizationModelFull();

		Cloudlet[] cloudlet = new Cloudlet[clc.size()];

		for(int i=0;i<clc.size();i++){
			long length = clc.get(i);
			cloudlet[i] = new Cloudlet(i, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
			// setting the owner of these Cloudlets
			cloudlet[i].setUserId(userId);
			list.add(cloudlet[i]);
		}

		return list;
	}
	
	private static List<Cloudlet> createGoogleCloudlet(int userId){
		
		LinkedList<Integer> clc = new LinkedList<>();
		int ll[] = {20, 40, 30, 4, 6};
		int lz[][] = {{15000, 55000}, {59000, 99000}, {101000, 135000}, {150000, 337500}, {525000, 900000}};
		
		for(int i = 0; i < ll.length; i++)
		{
			for(int j = 0; j < ll[i]; j++) {
				int milow = lz[i][0];
				int mihigh = lz[i][1];
				clc.add(milow + (j * ( (mihigh - milow) / (ll[i] - 1) ) ));
			}
		}
		
		// Creates a container to store Cloudlets
		LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();

		//cloudlet parameters
		long fileSize = 300;
		long outputSize = 300;
		int pesNumber = 1;
		UtilizationModel utilizationModel = new UtilizationModelFull();

		Cloudlet[] cloudlet = new Cloudlet[clc.size()];

		for(int i=0;i<clc.size();i++){
			long length = clc.get(i);
			cloudlet[i] = new Cloudlet(i, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
			// setting the owner of these Cloudlets
			cloudlet[i].setUserId(userId);
			list.add(cloudlet[i]);
		}

		return list;
	}
	
	private static HashMap<Integer, Double> getVmCrMap(List<Vm> vmlist) {
		HashMap<Integer, Double> vmcrmap = new HashMap<>();
		double total = 0;
		for(Vm vm: vmlist) {
			total += vm.getMips();
		}
		for(Vm vm: vmlist) {
			vmcrmap.put(vm.getId(), vm.getMips() / total);
		}
		return vmcrmap;
	}
	
	private static HashMap<Integer, Double> getVmShare(HashMap<Integer, Double> vmcrmap, long cloudletLength){
		HashMap<Integer, Double> vmshare = new HashMap<>();
		for(Entry<Integer, Double> vm: vmcrmap.entrySet()) {
			vmshare.put(vm.getKey(), vm.getValue() * cloudletLength);
		}
		return vmshare;
	}
	
	private static long getCloudletMISum(List<Cloudlet> cloudlist) {
		long sum = 0;
		
		for(Cloudlet cl: cloudlist)
			sum += cl.getCloudletTotalLength();
		
		return sum;
	}
	
	private static Integer getLargestVmShare(HashMap<Integer, Double> vmshare) {
		Double largest_vmshare = null;
		Integer id = null;
		
		for(Entry<Integer, Double> vm: vmshare.entrySet()) {
			if(Objects.isNull(largest_vmshare) || vm.getValue() > largest_vmshare) {
				largest_vmshare = vm.getValue();
				id = vm.getKey();
			}
		}
		return id;
	}
	
	private static Cloudlet getSmallestCloudlet(HashMap<Integer,Cloudlet> remainingCloudlet) {
		Cloudlet smallest_cl = null;
		for(Cloudlet cl: remainingCloudlet.values()) {
			if(Objects.isNull(smallest_cl) || cl.getCloudletLength() < smallest_cl.getCloudletLength()) {
				smallest_cl = cl;
			}
		}
		return smallest_cl;
	}
	
	private static Cloudlet getLargestCloudletToAssignVM(Double largest_vmshare, HashMap<Integer,Cloudlet> remainingCloudlet) {
		Cloudlet largest_cl = null;
		for(Cloudlet cl: remainingCloudlet.values()) {
			if(cl.getCloudletLength() < largest_vmshare) {
				if(Objects.isNull(largest_cl) || cl.getCloudletLength() > largest_cl.getCloudletLength()) {
					largest_cl = cl;
				}
			}
		}
		
		return largest_cl;
	}
	
	private static Cloudlet getLargestCloudlet(HashMap<Integer,Cloudlet> remainingCloudlet) {
		Cloudlet largest_cl = null;
		for(Cloudlet cl: remainingCloudlet.values()) {
			if(Objects.isNull(largest_cl) || cl.getCloudletLength() > largest_cl.getCloudletLength()) {
				largest_cl = cl;
			}
		
		}
		
		return largest_cl;
	}
	
	private static HashMap<Integer, Double> getVmCt(List<Vm> vmList, List<Cloudlet> cloudletList, Cloudlet cloudlet){
		HashMap<Integer, Double> vm_ct = new HashMap<>();
		for(Vm vm : vmList) {
			double sum = cloudlet == null ? 0 : cloudlet.getCloudletLength() / vm.getMips();
			for(Cloudlet cl : cloudletList) {
				if(cl.getVmId() == vm.getId())
					sum += (cl.getCloudletLength() / vm.getMips());
			}
			vm_ct.put(vm.getId(), sum);
		}
		return vm_ct;
	}
	
	private static HashMap<Integer, Double> getVmCt(List<Vm> vmList, List<Cloudlet> cloudletList){
		return getVmCt(vmList, cloudletList, null);
	}
	
	private static HashMap<Integer, Double> getCloudletCtPerVm(Cloudlet cl, List<Vm> vmList, HashMap<Integer, Double> vm_ct) {
		HashMap<Integer, Double> cloudlet_ct = new HashMap<>();
		
		for(Vm vm : vmList) {
			double sum = (cl.getCloudletLength() / vm.getMips()) + vm_ct.get(vm.getId());
			cloudlet_ct.put(vm.getId(), sum);
		}
		
		return cloudlet_ct;
	}
	
	private static Vm getVMwithEFT(HashMap<Integer, Double> cloudlet_ct_per_vm, List<Vm> vmList) {
		
		Double min = 0.0;
		if(vmList.size() == 0) return null;
		
		Vm vm = vmList.get(0);
		min = cloudlet_ct_per_vm.get(vm.getId());
		
		for(int i = 1; i < cloudlet_ct_per_vm.size(); i++) {
			if(cloudlet_ct_per_vm.get(vmList.get(i).getId()) < min) {
				vm = vmList.get(i);
				min = cloudlet_ct_per_vm.get(vmList.get(i).getId());
			}
		}
		
		return vm;
	}
	
	private static Double getAverageMakeSpan(HashMap<Integer, Double> vm_ct) {
		
		Double sum = 0.0;
		
		for(Double e: vm_ct.values()) {
			sum += e;
		}
		
		return sum / vm_ct.size();
	}
	
	private static HashMap<Integer, Double> getVmMakeSpan(List<Cloudlet> cloudletlist){
		HashMap<Integer, Double> vm_makespan = new HashMap<>();
		
		for(Cloudlet cl : cloudletlist) {
			if(cl.getVmId() != -1) {
				if(!vm_makespan.containsKey(cl.getVmId()) || cl.getFinishTime() > vm_makespan.get(cl.getVmId())) {
					vm_makespan.put(cl.getVmId(), cl.getFinishTime());
				}
			}
		}
		
		return vm_makespan;
	}
	
	private static Vm getVmWithMaxMakeSpan(HashMap<Integer, Double> vm_ct, List<Vm> vmlist) {
		
		Vm max = null;
		
		for(Vm vm : vmlist) {
			if(Objects.isNull(max) || vm_ct.get(vm.getId()) > vm_ct.get(max.getId()) ) {
				max = vm;
			}
		}
		
		return max;
		
	}
	
	private static Vm getVmWithMinMakeSpan(HashMap<Integer, Double> vm_ct, List<Vm> vmlist) {
		
		Vm min = null;
		
		for(Vm vm : vmlist) {
			if(Objects.isNull(min) || vm_ct.get(vm.getId()) < vm_ct.get(min.getId()) ) {
				min = vm;
			}
		}
		
		return min;
		
	}
	
	private static Vm getVmWithMinVmCt(Map<Integer, Double> vm_ct, List<Vm> vmlist, Cloudlet selectedCloudlet) {

		Vm min = null;

		for (Vm vm : vmlist) {
			if (Objects.isNull(min) || 
					vm_ct.get(vm.getId()) + (selectedCloudlet.getCloudletLength() / vm.getMips()) <
					vm_ct.get(min.getId()) + (selectedCloudlet.getCloudletLength() / min.getMips())
					) {
				min = vm;
			}
		}

		return min;

	}
	
	private static Cloudlet findMinDeadline(List<Cloudlet> cloudletList, Cloudlet selectedCloudlet, Vm vj, Map<Integer, Double> DEADLINES, double deadline) {
		Cloudlet cloudlet = null;
		Double minDt = null;
		
		for(int i = 0; i < cloudletList.size(); i++) {
			Cloudlet cl = cloudletList.get(i);
			
			// Past selectedCloudlet is non-scheduled
			if(cl.getCloudletId() == selectedCloudlet.getCloudletId()) break;
			
			// Must be for specified vm
			if(cloudlet.getVmId() != vj.getId()) continue;
			
			
			double candidateDeadline = DEADLINES.get(i);
			if(candidateDeadline > deadline) {
				if(minDt == null || candidateDeadline < minDt) {
					minDt = candidateDeadline;
					cloudlet = cl;
				}
			}
			
		}
		
		return cloudlet;
	}
	
	private static boolean SScheduler(List<Cloudlet> cloudletList, List<Vm> vmList, Map<Integer, Double> DEADLINES, Cloudlet selectedCloudlet, Vm vm, double deadline, double currentVmCt) {
		Cloudlet candidate = null;
		Integer posTc = null;
		int flag = 1;
		Double candidateExecTime = 0.0;
		Double candidateCt = 0.0;
		Double newCandidateCt = 0.0;

		Double newVmCt = 0.0;
		Double selectedNewCt = 0.0;
		
		List<Integer> sortedDeadline = sortDeadlines(DEADLINES, cloudletList, selectedCloudlet);
		
		if(sortedDeadline.size() == 0) return false;
		
		for(int i = 0; i < sortedDeadline.size(); i++) {
			
			Integer candidateId = sortedDeadline.get(i);
			Double candidateDeadline = DEADLINES.get(candidateId);
			Double selectedExecTime = selectedCloudlet.getCloudletLength() / vm.getMips();
			long candidateLength = 0;
			for(int ci = 0; ci < cloudletList.size(); ci++) {
				if(candidateId == cloudletList.get(ci).getCloudletId());
				candidateLength = cloudletList.get(ci).getCloudletLength();
			}
			candidateExecTime = candidateLength / vm.getMips();
			candidateCt = getCtofVm(cloudletList, vm, candidateId);
			newCandidateCt = candidateCt + currentVmCt;

			selectedNewCt = newCandidateCt - candidateExecTime;
			
			if(newCandidateCt < candidateDeadline && selectedNewCt < deadline) {
				
				for(int ci = 0; ci < cloudletList.size(); ci++) {
					if(candidateId == cloudletList.get(ci).getCloudletId())
						candidate = cloudletList.get(ci);
				}
				
				break;
			}
			
		}
				
		if(candidate != null) {
			
			posTc = cloudletList.indexOf(candidate);
			
			double sum = selectedNewCt;
			
			for(int i = posTc + 1; i < cloudletList.indexOf(selectedCloudlet); i++) {
				
				Cloudlet task = cloudletList.get(i);
				Integer taskId = task.getCloudletId();
				Double taskDeadline = DEADLINES.get(taskId);
									
				Double execTime = task.getCloudletLength() / vm.getMips();
				sum = sum + execTime;
				Double newCt = sum;
				
				if(newCt > taskDeadline) {
					flag = 0;
					break;
				}
				
			}
			
		} else return false;
		
		if(flag == 1) {
			cloudletList.remove(selectedCloudlet);
			cloudletList.add(posTc, selectedCloudlet);
			return true;
		}
		
		return false;
	}

	private static Double getCtofVm(List<Cloudlet> cloudletList, Vm vm, Integer uptoCandidateId) {
		Double sum = 0.0;
		
		for(int i = 0; i < cloudletList.size(); i++) {
			sum = sum + ( cloudletList.get(i).getCloudletLength() / vm.getMips() );
			if(cloudletList.get(i).getCloudletId() == uptoCandidateId) break;
		}
		
		return sum;
	}

	private static Datacenter createDatacenter(String name){

		// Here are the steps needed to create a PowerDatacenter:
		// 1. We need to create a list to store one or more
		//    Machines
		List<Host> hostList = new ArrayList<Host>();

		// 2. A Machine contains one or more PEs or CPUs/Cores. Therefore, should
		//    create a list to store these PEs before creating
		//    a Machine.
		List<Pe> peList1 = new ArrayList<Pe>();

		int mips = 4000;

		// 3. Create PEs and add these into the list.
		//for a quad-core machine, a list of 4 PEs is required:
		peList1.add(new Pe(0, new PeProvisionerSimple(mips))); // need to store Pe id and MIPS Rating
		peList1.add(new Pe(1, new PeProvisionerSimple(mips)));
		peList1.add(new Pe(2, new PeProvisionerSimple(mips)));
		peList1.add(new Pe(3, new PeProvisionerSimple(mips)));
		
		//Another list, for a dual-core machine
		List<Pe> peList2 = new ArrayList<Pe>();

		peList2.add(new Pe(0, new PeProvisionerSimple(mips)));
		peList2.add(new Pe(1, new PeProvisionerSimple(mips)));

		//4. Create Hosts with its id and list of PEs and add them to the list of machines
		int hostId = 0;
		int ram = 16384; //host memory (MB)
		long storage = 1000000; //host storage
		int bw = 10000;
		
		for(; hostId < 4; hostId++) {
			hostList.add(
    			new Host(
    				hostId,
    				new RamProvisionerSimple(ram),
    				new BwProvisionerSimple(bw),
    				storage,
    				peList1,
    				new VmSchedulerTimeShared(peList2)
	    		)
    		); // This is our first machine
		}
		
		for(; hostId < 26; hostId++) {
		hostList.add(
    			new Host(
    				hostId,
    				new RamProvisionerSimple(ram),
    				new BwProvisionerSimple(bw),
    				storage,
    				peList1,
    				new VmSchedulerTimeShared(peList1)
    			)
    		); // This is our first machine
		}


		//To create a host with a space-shared allocation policy for PEs to VMs:
		//hostList.add(
    	//		new Host(
    	//			hostId,
    	//			new CpuProvisionerSimple(peList1),
    	//			new RamProvisionerSimple(ram),
    	//			new BwProvisionerSimple(bw),
    	//			storage,
    	//			new VmSchedulerSpaceShared(peList1)
    	//		)
    	//	);

		//To create a host with a oportunistic space-shared allocation policy for PEs to VMs:
		//hostList.add(
    	//		new Host(
    	//			hostId,
    	//			new CpuProvisionerSimple(peList1),
    	//			new RamProvisionerSimple(ram),
    	//			new BwProvisionerSimple(bw),
    	//			storage,
    	//			new VmSchedulerOportunisticSpaceShared(peList1)
    	//		)
    	//	);


		// 5. Create a DatacenterCharacteristics object that stores the
		//    properties of a data center: architecture, OS, list of
		//    Machines, allocation policy: time- or space-shared, time zone
		//    and its price (G$/Pe time unit).
		String arch = "x86";      // system architecture
		String os = "Linux";          // operating system
		String vmm = "Xen";
		double time_zone = 10.0;         // time zone this resource located
		double cost = 3.0;              // the cost of using processing in this resource
		double costPerMem = 0.05;		// the cost of using memory in this resource
		double costPerStorage = 0.1;	// the cost of using storage in this resource
		double costPerBw = 0.1;			// the cost of using bw in this resource
		LinkedList<Storage> storageList = new LinkedList<Storage>();	//we are not adding SAN devices by now

		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);


		// 6. Finally, we need to create a PowerDatacenter object.
		Datacenter datacenter = null;
		try {
			datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return datacenter;
	}

	//We strongly encourage users to develop their own broker policies, to submit vms and cloudlets according
	//to the specific rules of the simulated scenario
	private static DatacenterBroker createBroker(){

		DatacenterBroker broker = null;
		try {
			broker = new DatacenterBroker("Broker");
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return broker;
	}

	/**
	 * Prints the Cloudlet objects
	 * @param list  list of Cloudlets
	 */
	private static void printCloudletList(List<Cloudlet> list) {
		int size = list.size();
		Cloudlet cloudlet;

		String indent = "    ";
		Log.printLine();
		Log.printLine("========== OUTPUT ==========");
		Log.printLine("Cloudlet ID" + indent + "STATUS" + indent +
				"Data center ID" + indent + "VM ID" + indent + indent + "Time" + indent + "Start Time" + indent + "Finish Time");

		DecimalFormat dft = new DecimalFormat("###.##");
		for (int i = 0; i < size; i++) {
			cloudlet = list.get(i);
			Log.print(indent + cloudlet.getCloudletId() + indent + indent);

			if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS){
				Log.print("SUCCESS");

				Log.printLine( indent + indent + cloudlet.getResourceId() + indent + indent + indent + cloudlet.getVmId() +
						indent + indent + indent + dft.format(cloudlet.getActualCPUTime()) +
						indent + indent + dft.format(cloudlet.getExecStartTime())+ indent + indent + indent + dft.format(cloudlet.getFinishTime()));
			}
		}

	}
	
	////////////////////////// STATIC METHODS ///////////////////////

	/**
	 * Creates main() to run this example
	 */
	public static void run(int n) {
		
		Log.printLine("Starting " + MethodHandles.lookup().lookupClass().getSimpleName() + "...");
		
		/** The cloudlet list. */
		List<Cloudlet> cloudletList;
		
		Map<Integer, Double> DEADLINES = new HashMap<>();

		/** The vmlist. */
		List<Vm> vmList;

		try {
			// First step: Initialize the CloudSim package. It should be called
			// before creating any entities.
			int num_user = 1; // number of grid users
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false; // mean trace events

			// Initialize the CloudSim library
			CloudSim.init(num_user, calendar, trace_flag);

			// Second step: Create Datacenters
			// Datacenters are the resource providers in CloudSim. We need at list one of
			// them to run a CloudSim simulation
			@SuppressWarnings("unused")
			Datacenter datacenter0 = createDatacenter("Datacenter_0");

			// Third step: Create Broker
			DatacenterBroker broker = createBroker();
			int brokerId = broker.getId();

			// Fourth step: Create VMs and Cloudlets and send them to broker
			// vmlist = createVM(brokerId,30); //creating 20 vms
			// cloudletList = createCloudlet(brokerId,2000); // creating 40 cloudlets

			vmList = createExperimentVM(brokerId);
			if (WORKLOAD == 0)
				cloudletList = createSyntheticCloudlet(brokerId);
			else if (WORKLOAD == 1)
				cloudletList = createGoogleCloudlet(brokerId);
			else {
				cloudletList = createSyntheticCloudlet(brokerId);
				cloudletList.addAll(createGoogleCloudlet(brokerId));
			}

			broker.submitVmList(vmList);
			broker.submitCloudletList(cloudletList);
			
			Map<Integer, Vm> vmMap = new HashMap<>();
			
			for(int i = 0; i < vmList.size(); i++) {
				vmMap.put(vmList.get(i).getId(), vmList.get(i));
			}

			List<Cloudlet> rejectedCloudlet = new ArrayList<>();
			for (Cloudlet cl : cloudletList) {
				DEADLINES.put(cl.getCloudletId(), genRandomDouble(DEADLINE_MIN, DEADLINE_MAX));
			}

			// CODE STARTS HERE

			// FILL
			if (SCHEDULER == 3) {
				HashMap<Integer, Double> vmcrmap = getVmCrMap(vmList);
				Long cloudletLength = getCloudletMISum(cloudletList);
				HashMap<Integer, Double> vmshare = getVmShare(vmcrmap, cloudletLength);
				HashMap<Integer, Cloudlet> remainingCloudlet = new HashMap<>();
				HashMap<Integer, Integer> clAssignedToVM = new HashMap<>();

				for (Cloudlet cl : cloudletList)
					remainingCloudlet.put(cl.getCloudletId(), cl);

				while (true) {

					Cloudlet cl = getSmallestCloudlet(remainingCloudlet);

					if (Objects.isNull(cl))
						break;

					int largest_vmshare_id = getLargestVmShare(vmshare);

					if (!(cl.getCloudletLength() >= vmshare.get(largest_vmshare_id)))
						break;

					Cloudlet clToAssign = getLargestCloudletToAssignVM(vmshare.get(largest_vmshare_id),
							remainingCloudlet);

					remainingCloudlet.remove(clToAssign.getCloudletId());

					boolean rejected = false;

					if (getVmCt(vmList, cloudletList, clToAssign).get(largest_vmshare_id) > DEADLINES
							.get(clToAssign.getCloudletId())) {
						rejectedCloudlet.add(clToAssign);
						rejected = true;

						if (ALLOW_REJECTS_TO_RUN == 0) {
							cloudletList.remove(cl);
							broker.getCloudletList().remove(cl);
						}
					}
					if (ALLOW_REJECTS_TO_RUN == 1 || !rejected) {

						vmshare.put(largest_vmshare_id, vmshare.get(largest_vmshare_id) - cl.getCloudletLength());
						broker.bindCloudletToVm(clToAssign.getCloudletId(), largest_vmshare_id);
						clAssignedToVM.put(clToAssign.getCloudletId(), largest_vmshare_id);
					}

					if (remainingCloudlet.size() == 0)
						break;
				}

				// SPILL

				while (remainingCloudlet.size() != 0) {

					Cloudlet cl = getLargestCloudlet(remainingCloudlet);

					HashMap<Integer, Double> vm_ct = getVmCt(vmList, cloudletList);
					HashMap<Integer, Double> cloudlet_ct_per_vm = getCloudletCtPerVm(cl, vmList, vm_ct);

					Vm vmwitheft = getVMwithEFT(cloudlet_ct_per_vm, vmList);
					int vmid = vmwitheft.getId();

					remainingCloudlet.remove(cl.getCloudletId());

					boolean rejected = false;

					if (getVmCt(vmList, cloudletList, cl).get(vmid) > DEADLINES.get(cl.getCloudletId())) {
						rejectedCloudlet.add(cl);
						rejected = true;
						if (ALLOW_REJECTS_TO_RUN == 0) {
							cloudletList.remove(cl);
							broker.getCloudletList().remove(cl);
						}
					}
					if (ALLOW_REJECTS_TO_RUN == 1 || !rejected) {
						broker.bindCloudletToVm(cl.getCloudletId(), vmwitheft.getId());
						clAssignedToVM.put(cl.getCloudletId(), vmwitheft.getId());
					}
				}
			}

			// CODE ENDS HERE FOR SCHEDULER, RESUMED LATER

			// RANDOM
			else if (SCHEDULER == 0) {
				for (int i = 0; i < cloudletList.size(); i++) {
					int vmid = genRandom(0, vmList.size() - 1);
					Cloudlet cl = cloudletList.get(i);
					boolean rejected = false;
					if (getVmCt(vmList, cloudletList, cl).get(vmid) > DEADLINES.get(cl.getCloudletId())) {
						rejectedCloudlet.add(cloudletList.get(i));
						rejected = true;
						if (ALLOW_REJECTS_TO_RUN == 0) {
							cloudletList.remove(cl);
							broker.getCloudletList().remove(cl);
							i--;
						}
					}
					if (ALLOW_REJECTS_TO_RUN == 1 || !rejected) {
						broker.bindCloudletToVm(cl.getCloudletId(), vmList.get(vmid).getId());
					}
				}
			}

			// Round Robin
			else if (SCHEDULER == 1) {
				int vmid = 0;
				for (int i = 0; i < cloudletList.size(); i++) {
					boolean rejected = false;
					Cloudlet cl = cloudletList.get(i);
					if (getVmCt(vmList, cloudletList, cl).get(vmid) > DEADLINES.get(cl.getCloudletId())) {
						rejectedCloudlet.add(cloudletList.get(i));
						rejected = true;
						if (ALLOW_REJECTS_TO_RUN == 0) {
							cloudletList.remove(cl);
							broker.getCloudletList().remove(cl);
							i--;
						}
					}
					if (ALLOW_REJECTS_TO_RUN == 1 || !rejected) {
						broker.bindCloudletToVm(cl.getCloudletId(), vmList.get(vmid).getId());
						vmid = (vmid + 1) % vmList.size();
					}
				}
			}

			// Minimum Completion Time
			else if (SCHEDULER == 2) {
				for(int i = 0; i < cloudletList.size(); i++) {
					HashMap<Integer, Double> vmCt = getVmCt(vmList, cloudletList, cloudletList.get(i));
					boolean rejected = false;
					int vmId = getVmWithMinMakeSpan(vmCt, vmList).getId();
					Cloudlet cl = cloudletList.get(i);
					if (vmCt.get(vmId) > DEADLINES.get(cl.getCloudletId())) {
						rejectedCloudlet.add(cl);
						rejected = true;
						if (ALLOW_REJECTS_TO_RUN == 0) {
							cloudletList.remove(cl);
							broker.getCloudletList().remove(cl);
							i--;
						}
					}
					if (ALLOW_REJECTS_TO_RUN == 1 || !rejected) {
						broker.bindCloudletToVm(cl.getCloudletId(), vmList.get(vmId).getId());
					}
				}
			}
			
			// OGRADL
			else if (SCHEDULER == 4) {
				int count1 = 0;
				int count2 = 0;
				Map<Integer, Double> vmctMap = getVmCt(vmList, cloudletList);

				for(int i = 0; i < cloudletList.size(); i++) {
					
					boolean rejected = false;
					boolean foundVm = false;
					Integer assignedVmId = -1;
					
					Cloudlet selectedCloudlet = cloudletList.get(i);
					
					Vm mctVM = getVmWithMinVmCt(vmctMap, vmList, selectedCloudlet);
//					List<Integer> vmctSortedIds = sortVmCt(vmctMap, vmMap, selectedCloudlet);
//					Vm mctVM = null;
//					if(vmctSortedIds.size() != 0) {
//						mctVM = vmMap.get(vmctSortedIds.get(0));
//					}
					
					if(mctVM != null) {
					
						double cloudletCt = selectedCloudlet.getCloudletLength() / mctVM.getMips();
						double currentVmCt = vmctMap.get(mctVM.getId());
						double newVmCt = currentVmCt + cloudletCt;
						double deadline = DEADLINES.get(selectedCloudlet.getCloudletId());
	
						// TODO Initial check
						if (newVmCt <= deadline) {
							foundVm = true;
							assignedVmId = mctVM.getId();
							count1 = count1 + 1;
						}
					}
					
					if(!foundVm) {
						
						boolean findSPQ = false;
						Vm vj = null;
						
						double deadline = DEADLINES.get(selectedCloudlet.getCloudletId());
						
						List<Integer> vmctSortedId = sortVmCt(vmctMap, vmMap, selectedCloudlet);
						
						for(int vi = 0; vi < vmctSortedId.get(vi); vi++) {
							
							vj = vmMap.get(vmctSortedId.get(vi));
							double currentVmCt = vmctMap.get(vj.getId());
							
							if(findSPQ == true) {
								break;
							}
							
							findSPQ = SScheduler(cloudletList, vmList, DEADLINES, selectedCloudlet, vj, deadline, currentVmCt);
						
							if(findSPQ == true) {
								foundVm = true;
								assignedVmId = vj.getId();
								count2 = count2+1;
								break;
							} 
							
						}
						
						if(findSPQ == false) {
							rejectedCloudlet.add(selectedCloudlet);
							rejected = true;
						}
						
					}
					
					if (!foundVm) {
						if (ALLOW_REJECTS_TO_RUN == 0) {
							cloudletList.remove(selectedCloudlet);
							broker.getCloudletList().remove(selectedCloudlet);
							i--;
						}
					}
					
					if (ALLOW_REJECTS_TO_RUN == 1 || !rejected) {
						if(assignedVmId == -1) {
							assignedVmId = mctVM.getId();
						}
						broker.bindCloudletToVm(selectedCloudlet.getCloudletId(), 
								vmList.get(assignedVmId).getId());
						double cloudletCt = selectedCloudlet.getCloudletLength() / vmMap.get(assignedVmId).getMips();
						double newVmCt = vmctMap.get(assignedVmId) + cloudletCt;
						vmctMap.put(assignedVmId, newVmCt);
					}
										
				}
				System.out.println("Count 1 " + String.valueOf(count1));
				System.out.println("Count 2 " + String.valueOf(count2));
			}
			
			// Fifth step: Starts the simulation
			CloudSim.startSimulation();

			// Final step: Print results when simulation is over
			List<Cloudlet> newList = broker.getCloudletReceivedList();

			CloudSim.stopSimulation();

			//printCloudletList(newList);

			// CODE RESUMES HERE

			// Metrics

			
			HashMap<Integer, Double> vmCt = getVmCt(vmList, cloudletList);
			HashMap<Integer, Double> vm_makespan = getVmMakeSpan(cloudletList);
			Vm vmWitHighestMakeSpan = getVmWithMaxMakeSpan(vmCt, vmList);
			Double average_makespan = getAverageMakeSpan(vmCt);
			Double makespan = vmCt.get(vmWitHighestMakeSpan.getId());
			Double throughput = newList.size() / makespan;
			Double arur = average_makespan / makespan;
			Integer totalTasks = cloudletList.size() + rejectedCloudlet.size();
			if(ALLOW_REJECTS_TO_RUN == 1) {
				totalTasks = cloudletList.size();
			}
			Double rejectionRate = (double) ((rejectedCloudlet.size() * 100) / totalTasks);
			Double acceptedRate = 100.0 - rejectionRate;

			System.out.println(String.format("Makespan: %f", makespan));
			System.out.println(String.format("Throughput: %f", throughput));
			System.out.println(String.format("ARUR: %f", arur));

			System.out.println(String.format("Rejection Rate: %f", rejectionRate));
			System.out.println(String.format("Acceptance Rate: %f", acceptedRate));
			
			t_makespan[n] = makespan;
			t_throughput[n] = throughput;
			t_arur[n] = arur;
			t_accept[n] = acceptedRate;
			t_reject[n] = rejectionRate;

			// CODE ENDS HERE

			String scheduler_names[] = { "RALBA", "Random Selection", "Round Robin", "Minimum Completion Time" };
			String scheduler_name = "Unknown";
			
			scheduler_name = SCHEDULER < scheduler_names.length ? scheduler_name = scheduler_names[SCHEDULER] : scheduler_names[scheduler_names.length - 1];

			Log.printLine(scheduler_name + " finished!");
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("The simulation has been terminated due to an unexpected error");
		}
	}

	private static List<Integer> sortVmCt(Map<Integer, Double> vmct, Map<Integer, Vm> vmMap, Cloudlet selectedCloudlet) {
		List<Integer> lvmct = new LinkedList<Integer>(vmct.keySet());

		/*
		 * Since these are objects, java uses Timsort by default which has complexity
		 * Best Case: n
		 * Average Case: nlogn
		 * Worst Case: nlogn
		 * Memory Complexity: n
		 * Stable: Yes
		 */

		lvmct.sort(
				(o1, o2) -> ((Double) (vmct.get(o1) + (selectedCloudlet.getCloudletLength() / vmMap.get(o1).getMips())))
						.compareTo((Double) (vmct.get(o2)
								+ (selectedCloudlet.getCloudletLength() / vmMap.get(o2).getMips()))));

		return lvmct;
	}
	
	private static List<Integer> sortDeadlines(Map<Integer, Double> deadlines, List<Cloudlet> cloudletList, Cloudlet selectedCloudlet) {

		/*
		 * Since these are objects, java uses Timsort by default which has complexity
		 * Best Case: n
		 * Average Case: nlogn
		 * Worst Case: nlogn
		 * Memory Complexity: n
		 * Stable: Yes
		 */
		
	
		List<Integer> Ldeadlines = new LinkedList<Integer>();
		
		for(int i = 0; i < cloudletList.size(); i++) {
			
			Integer clid = cloudletList.get(i).getCloudletId();
			
			if(clid == selectedCloudlet.getCloudletId()) break;
			
			if(deadlines.get(clid) < deadlines.get(selectedCloudlet.getCloudletId())) continue;
			
			Ldeadlines.add(clid);
		}

		Ldeadlines.sort(
				(o1, o2) -> deadlines.get(o1).compareTo(deadlines.get(o2))
		);

		return Ldeadlines;
	}

	public static Double getAvg(Double[] args) {
		Double sum = 0.0;
		for(Double arg: args) {
			sum = sum + arg;
		}
		return sum / args.length;
	}
	
	public static void main(String[] args) {
		
		if(WORKLOAD == 0) {
			DEADLINE_MIN = DEADLINE_SYN_MIN;
			DEADLINE_MAX = DEADLINE_SYN_MAX;
		} else {
			DEADLINE_MIN = DEADLINE_GOOGLE_MIN;
			DEADLINE_MAX = DEADLINE_GOOGLE_MAX;
		}
		
		for(int i = 0; i < t_count; i++) {
			run(i);
		}
		
		System.out.println(String.format("Makespan: %f", getAvg(t_makespan)));
		System.out.println(String.format("Throughput: %f", getAvg(t_throughput)));
		System.out.println(String.format("ARUR: %f", getAvg(t_arur)));

		System.out.println(String.format("Rejection Rate: %f", getAvg(t_reject)));
		System.out.println(String.format("Acceptance Rate: %f", getAvg(t_accept)));
		
		System.out.println(String.format("%f", getAvg(t_makespan)));
		System.out.println(String.format("%f", getAvg(t_throughput)));
		System.out.println(String.format("%f", getAvg(t_arur)));

		System.out.println(String.format("%f", getAvg(t_reject)));
		System.out.println(String.format("%f", getAvg(t_accept)));
		
	}
	
}
