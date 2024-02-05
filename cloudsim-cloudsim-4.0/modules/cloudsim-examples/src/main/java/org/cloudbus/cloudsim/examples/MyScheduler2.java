package org.cloudbus.cloudsim.examples;

import java.lang.invoke.MethodHandles;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

public class MyScheduler2 {
	// Tried to make faster method for adjustment

	private static int WORKLOAD = 0;
	// 0 - Synthetic
	// 1 - Google like
	// 2 - Synthetic + Google Like

	private static int SORT_TYPE = 2;
	// 0 - No Sort
	// 1 - Deadline
	// 2 - Deadline / Length
	// 3 - Length / Deadline

	private static int ADJUST_DEEP_SEARCH = 1;
	// 0 - Adjust Check will make sure both deadline and exec time are satisfied at
	// all indexes
	// 1 - Adjust Check will only make sure that exec time of candidate is satisfied
	// at all interval, only candidate which satisfies deadline is selected
	
	private static int ALLOW_REJECTS_TO_RUN = 0;
	// 0 - Will not let rejected task run
	// 1 - Will let rejected task run
	
	private static double PREACQUIRED_VM = 1.0;
	// How many VM should be available from the start
	// 0 is none while 1 is all.
	
	private static boolean FIND_NEW_VM_WITHIN_DEADLINE = true;
	// true - allocate those VM that can execute within deadline
	// false - allocate any VM that can finish it quickly

	private static int COUNT_ASSIGN = 0;
	private static int COUNT_ADJUST = 0;
	private static int COUNT_ACQUIRE = 0;
	private static int COUNT_FORCED = 0;
	
	// 1-30 SYN
	// 100-400 GOOGLE
	private static double DEADLINE_MIN = 100.0;
	private static double DEADLINE_MAX = 400.0;
	
	private static double DEADLINE_GOOGLE_MIN = 100.0;
	private static double DEADLINE_GOOGLE_MAX = 400.0;
	private static double DEADLINE_SYN_MIN = 2.0;
	private static double DEADLINE_SYN_MAX = 8.0;

	private static double BOOT_TIME_MIN = 0.0;
	private static double BOOT_TIME_MAX = 0.0;

	private static double ACQUISITION_DELAY_MIN = 0.0;
	private static double ACQUISITION_DELAY_MAX = 0.0;
	
	public static int t_count = 100;
	public static Double[] t_makespan = new Double[t_count];
	public static Double[] t_throughput = new Double[t_count];
	public static Double[] t_arur = new Double[t_count];
	public static Double[] t_accept = new Double[t_count];
	public static Double[] t_reject = new Double[t_count];
	public static Integer[] t_assign = new Integer[t_count];
	public static Integer[] t_adjust = new Integer[t_count];
	public static Integer[] t_acquired = new Integer[t_count];
	public static Integer[] t_forced = new Integer[t_count];
	
	public MyScheduler2() {
		if(WORKLOAD == 0) {
			DEADLINE_MIN = 2.0;
			DEADLINE_MAX = 8.0;
		} else {
			DEADLINE_MIN = 100.0;
			DEADLINE_MAX = 400.0;
		}
	}

	public static int genRandom(int low, int high) {
		return new Random().nextInt(high + 1 - low) + low;
	}

	public static Double genRandomDouble(int low, int high) {
		return low + (high - low) * new Random().nextDouble();
	}

	public static Double genRandomDouble(double low, double high) {
		return low + (high - low) * new Random().nextDouble();
	}

	private static EVm createVM(int id, int idShift, int userId, int mips) {
		// VM Parameters
		long size = 10000; // image size (MB)
		int ram = 512; // vm memory (MB)
		long bw = 1000;
		int pesNumber = 1; // number of cpus
		String vmm = "Xen"; // VMM name
		return new EVm(id + idShift, userId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared(),
				genRandomDouble(BOOT_TIME_MIN, BOOT_TIME_MAX),
				genRandomDouble(ACQUISITION_DELAY_MIN, ACQUISITION_DELAY_MAX));
	}

	private static List<EVm> createVM(int id, int idShift, int userId, int mips, int number) {
		LinkedList<EVm> list = new LinkedList<EVm>();
		for (int i = 0; i < number; i++)
			list.add(createVM(id + i, idShift, userId, mips));
		return list;
	}

	private static List<EVm> createVMAdvance(int userId, int vms, int mipslow, int mipshigh) {

		// Creates a container to store VMs. This list is passed to the broker later
		LinkedList<EVm> list = new LinkedList<EVm>();

		// create VMs
		EVm[] vm = new EVm[vms];

		for (int i = 0; i < vms; i++) {
			int mips = MyScheduler.genRandom(mipslow, mipshigh);
			vm[i] = createVM(i, 0, userId, mips);
			// for creating a VM with a space shared scheduling policy for cloudlets:
			// vm[i] = Vm(i, userId, mips, pesNumber, ram, bw, size, priority, vmm, new
			// CloudletSchedulerSpaceShared());

			list.add(vm[i]);
		}

		return list;
	}

	private static List<EVm> createExperimentVM(int userId) {

		// Creates a container to store VMs. This list is passed to the broker later
		LinkedList<EVm> list = new LinkedList<EVm>();

		LinkedList<Integer> vmc = new LinkedList<>();
		int ll[] = { 7, 6, 6, 6, 6, 6, 6 };
		int lz[] = { 500, 750, 1000, 1250, 1500, 1750, 4000 };
		;

		for (int i = 0; i < ll.length; i++) {
			for (int j = 0; j < ll[i]; j++) {
				vmc.add(lz[i]);
			}
		}

		// create VMs
		EVm[] vm = new EVm[vmc.size()];

		for (int i = 0; i < vmc.size(); i++) {
			vm[i] = createVM(i, 0, userId, vmc.get(i));
			// for creating a VM with a space shared scheduling policy for cloudlets:
			// vm[i] = Vm(i, userId, mips, pesNumber, ram, bw, size, priority, vmm, new
			// CloudletSchedulerSpaceShared());

			list.add(vm[i]);
		}

		return list;
	}

	private static List<ECloudlet> createCloudletAdvance(int userId, int cloudlets, int milow, int mihigh) {
		// Creates a container to store Cloudlets
		LinkedList<ECloudlet> list = new LinkedList<ECloudlet>();

		ECloudlet[] cloudlet = new ECloudlet[cloudlets];

		for (int i = 0; i < cloudlets; i++) {
			long length = milow + (i * ((mihigh - milow) / (cloudlets - 1)));
			cloudlet[i] = createCloudlet(i, 0, length);
			// setting the owner of these Cloudlets
			cloudlet[i].setUserId(userId);
			list.add(cloudlet[i]);
		}

		return list;
	}

	private static ECloudlet createCloudlet(int id, int idShift, long length) {
		// cloudlet parameters
		long fileSize = 300;
		long outputSize = 300;
		int pesNumber = 1;
		UtilizationModel utilizationModel = new UtilizationModelFull();

		return new ECloudlet(id + idShift, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel,
				utilizationModel, 0, genRandomDouble(DEADLINE_MIN, DEADLINE_MAX));
	}

	private static List<ECloudlet> createSyntheticCloudlet(int userId) {

		LinkedList<Integer> clc = new LinkedList<>();
		int ll[] = { 60, 5, 10, 5 };
		int lz[][] = { { 800, 1200 }, { 1800, 2500 }, { 7000, 10000 }, { 30000, 45000 } };

		for (int i = 0; i < ll.length; i++) {
			for (int j = 0; j < ll[i]; j++) {
				int milow = lz[i][0];
				int mihigh = lz[i][1];
				clc.add(milow + (j * ((mihigh - milow) / (ll[i] - 1))));
			}
		}

		// Creates a container to store Cloudlets
		LinkedList<ECloudlet> list = new LinkedList<ECloudlet>();

		ECloudlet[] cloudlet = new ECloudlet[clc.size()];

		for (int i = 0; i < clc.size(); i++) {
			long length = clc.get(i);
			cloudlet[i] = createCloudlet(i, 0, length);
			// setting the owner of these Cloudlets
			cloudlet[i].setUserId(userId);
			list.add(cloudlet[i]);
		}

		return list;
	}

	private static List<ECloudlet> createGoogleCloudlet(int userId) {

		LinkedList<Integer> clc = new LinkedList<>();
		int ll[] = { 20, 40, 30, 4, 6 };
		int lz[][] = { { 15000, 55000 }, { 59000, 99000 }, { 101000, 135000 }, { 150000, 337500 }, { 525000, 900000 } };

		for (int i = 0; i < ll.length; i++) {
			for (int j = 0; j < ll[i]; j++) {
				int milow = lz[i][0];
				int mihigh = lz[i][1];
				clc.add(milow + (j * ((mihigh - milow) / (ll[i] - 1))));
			}
		}

		// Creates a container to store Cloudlets
		LinkedList<ECloudlet> list = new LinkedList<ECloudlet>();

		ECloudlet[] cloudlet = new ECloudlet[clc.size()];

		for (int i = 0; i < clc.size(); i++) {
			long length = clc.get(i);
			cloudlet[i] = createCloudlet(i, 0, length);
			// setting the owner of these Cloudlets
			cloudlet[i].setUserId(userId);
			list.add(cloudlet[i]);
		}

		return list;
	}

	private static long getCloudletMISum(List<ECloudlet> cloudlist) {
		long sum = 0;

		for (ECloudlet cl : cloudlist)
			sum += cl.getCloudletTotalLength();

		return sum;
	}

	private static ECloudlet getSmallestCloudlet(Map<Integer, ECloudlet> remainingCloudlet) {
		ECloudlet smallest_cl = null;
		for (ECloudlet cl : remainingCloudlet.values()) {
			if (Objects.isNull(smallest_cl) || cl.getCloudletLength() < smallest_cl.getCloudletLength()) {
				smallest_cl = cl;
			}
		}
		return smallest_cl;
	}

	private static ECloudlet getLargestCloudletToAssignVM(Double largest_vmshare,
			HashMap<Integer, ECloudlet> remainingCloudlet) {
		ECloudlet largest_cl = null;
		for (ECloudlet cl : remainingCloudlet.values()) {
			if (cl.getCloudletLength() < largest_vmshare) {
				if (Objects.isNull(largest_cl) || cl.getCloudletLength() > largest_cl.getCloudletLength()) {
					largest_cl = cl;
				}
			}
		}

		return largest_cl;
	}

	private static ECloudlet getLargestCloudlet(Map<Integer, ECloudlet> remainingCloudlet) {
		ECloudlet largest_cl = null;
		for (ECloudlet cl : remainingCloudlet.values()) {
			if (Objects.isNull(largest_cl) || cl.getCloudletLength() > largest_cl.getCloudletLength()) {
				largest_cl = cl;
			}

		}

		return largest_cl;
	}

	/**
	 * Generates acquistion delay using random number generation.
	 * 
	 * @param min Minimum range
	 * @param max Maximum range
	 * @return Double value within specified range
	 */
	private static double genRandomAcquisitionDelay(int min, int max) {
		Random r = new Random();
		return min + (max - min) * r.nextDouble();
	}

	/**
	 * Generates acquistion delay using random number generation from 0 to max
	 * range.
	 * 
	 * @param max Maximum range
	 * @return Double value from 0 to specified range
	 */
	private static double genRandomAcquisitionDelay(int max) {
		return genRandomAcquisitionDelay(0, max);
	}

	/**
	 * Calculates the completion time for each VM.
	 * 
	 * @param vmList           List of VM
	 * @param cloudletList     List of Cloudlets
	 * @param selectedCloudlet execution for this cloudlet will be added to each VM.
	 *                         Set the value to null if you only want current
	 *                         completion time
	 * @return HashMap< Int, Double > VM.ID, Completion Time
	 */
	private static HashMap<Integer, Double> getVmCt(List<EVm> vmList, List<ECloudlet> cloudletList,
			ECloudlet selectedCloudlet) {
		HashMap<Integer, Double> vm_ct = new HashMap<>();
		for (EVm vm : vmList) {
			double selectedExecTime = Objects.isNull(selectedCloudlet) ? 0
					: (selectedCloudlet.getCloudletLength() / vm.getMips());
			// We will add both acquisition delay and boot time delay
			double sum = vm.getAcquisitionDelay() + vm.getBootTime() + selectedExecTime;
			for (ECloudlet cl : cloudletList) {
				if (cl.getVmId() == vm.getId())
					sum += (cl.getCloudletLength() / vm.getMips());
			}
			vm_ct.put(vm.getId(), sum);
		}
		return vm_ct;
	}

	/**
	 * Calculates the completion time for each VM.
	 * 
	 * @param vmList       List of VM
	 * @param cloudletList List of Cloudlets
	 * @return HashMap< Int, Double > VM.ID, Completion Time
	 */
	private static HashMap<Integer, Double> getVmCt(List<EVm> vmList, List<ECloudlet> cloudletList) {
		return getVmCt(vmList, cloudletList, null);
	}
	
	/**
	 * Checks the list of available cloudlets and returns the best candidate that
	 * can complete the task in minimum time. You can specify if the task must complete
	 * within the deadline using the onlyWithinDeadline param.
	 * 
	 * @param availableVmList  List of available VM
	 * @param selectedCloudlet Cloudlet to check for
	 * @param onlyWithinDeadline find those VM that complete the task within deadline.
	 * @return available Vm
	 */
	private static EVm checkAvailableVms(List<EVm> availableVmList, ECloudlet selectedCloudlet, boolean onlyWithinDeadline) {
		EVm acquiredVm = null;
		Double minVmCt = null;
		for (EVm availableVm : availableVmList) {
			Double availaleVmCt = (selectedCloudlet.getCloudletLength() / availableVm.getMips())
					+ availableVm.getBootTime() + availableVm.getAcquisitionDelay();
			if ((onlyWithinDeadline == false || availaleVmCt <= selectedCloudlet.getDeadlineTime())
					&& (Objects.isNull(minVmCt) || availaleVmCt < minVmCt)) {
				acquiredVm = availableVm;
				minVmCt = availaleVmCt;
			}
		}
		return acquiredVm;
	}
	

	/**
	 * Checks the list of available cloudlets and returns the best candidate that
	 * can complete the task in minimum time within the deadline.
	 * 
	 * @param availableVmList  List of available VM
	 * @param selectedCloudlet Cloudlet to check for
	 * @return available Vm
	 */
	private static EVm checkAvailableVms(List<EVm> availableVmList, ECloudlet selectedCloudlet) {
		return checkAvailableVms(availableVmList, selectedCloudlet, true);
	}

	/**
	 * Sort the VM by their completion time and return the Ids.
	 * 
	 * @param vmct Map of VM.Id to calculated completion time
	 * @return List of VM.Id
	 */
	private static List<Integer> sortVmCt(Map<Integer, Double> vmct, Map<Integer, EVm> vmMap,
			ECloudlet selectedCloudlet) {
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

	private static ECloudlet checkAdjustPossible(ECloudlet cloudlet, List<ECloudlet> cloudletList, EVm vm,
			Double vmct) {

		ECloudlet selectedCloudlet = null;
		double cloudletExecTime = cloudlet.getCloudletLength() / vm.getMips();

		for (int i = cloudletList.size() - 1; i >= 0; i--) {

			ECloudlet candidate = cloudletList.get(i);

			if (candidate.getCloudletId() == cloudlet.getCloudletId())
				continue;
			if (candidate.getVmId() != vm.getId())
				continue;
			if (candidate.getStatus() == Cloudlet.SUCCESS || candidate.getStatus() == Cloudlet.RESUMED
					|| candidate.getStatus() == Cloudlet.INEXEC)
				continue;

			double candidateExecTime = candidate.getCloudletLength() / vm.getMips();
			double newCandidateVmct = vmct + cloudletExecTime;
			double newCloudletVmct = vmct - candidateExecTime + cloudletExecTime;
			boolean newCloudletSatisfied = newCloudletVmct <= cloudlet.getDeadlineTime();
			boolean newCandidateSatisfied = newCandidateVmct <= candidate.getDeadlineTime();

			if (ADJUST_DEEP_SEARCH == 0) {

				if (newCloudletSatisfied && newCandidateSatisfied) {
					vmct = vmct - candidateExecTime;
					selectedCloudlet = candidate;
				} else {
					break;
				}

			} else if (ADJUST_DEEP_SEARCH == 1) {

				if (newCandidateSatisfied) {
					vmct = vmct - candidateExecTime;
					if (newCloudletSatisfied) {
						selectedCloudlet = candidate;
					}
				} else {
					break;
				}

			}
		}

		return selectedCloudlet;
	}
	
	private static ECloudlet checkAdjustPossibleV2(ECloudlet cloudlet, List<ECloudlet> cloudletList, Map<Integer, EVm> vmMap, Map<Integer, Double> vmCtMap) {
		
		Map<Integer, ECloudlet> vmCloudletHistory = new HashMap<>();
		Map<Integer, Double> newVmCtMap = new HashMap<>();
		EVm vm = null;
		
		for(Integer vmId : vmMap.keySet()) {
			newVmCtMap.put(vmId, vmCtMap.get(vmId) + (cloudlet.getCloudletLength() / vmMap.get(vmId).getMips()));
		}
		
		for (int i = cloudletList.size() - 1; i >= 0; i--) {

			ECloudlet candidate = cloudletList.get(i);

			if (candidate.getCloudletId() == cloudlet.getCloudletId())
				continue;
			if(candidate.getVmId() == -1)
				continue;
			if (candidate.getStatus() == Cloudlet.SUCCESS || candidate.getStatus() == Cloudlet.RESUMED
					|| candidate.getStatus() == Cloudlet.INEXEC)
				continue;
			
			vm = vmMap.get(candidate.getVmId());
			double vmct = newVmCtMap.get(vm.getId());

			double cloudletExecTime = cloudlet.getCloudletLength() / vm.getMips();
			double candidateExecTime = candidate.getCloudletLength() / vm.getMips();
			double newCandidateVmct = vmct + cloudletExecTime;
			double newCloudletVmct = vmct - candidateExecTime + cloudletExecTime;
			boolean newCloudletSatisfied = newCloudletVmct <= cloudlet.getDeadlineTime();
			boolean newCandidateSatisfied = newCandidateVmct <= candidate.getDeadlineTime();

			if (ADJUST_DEEP_SEARCH == 0) {

				if (newCloudletSatisfied && newCandidateSatisfied) {
					vmct = vmct - candidateExecTime;
					newVmCtMap.put(vm.getId(), vmct);
					vmCloudletHistory.put(vm.getId(), candidate);
				} else {
					break;
				}

			} else if (ADJUST_DEEP_SEARCH == 1) {

				if (newCandidateSatisfied) {
					vmct = vmct - candidateExecTime;
					newVmCtMap.put(vm.getId(), vmct);
					if (newCloudletSatisfied) {
						vmCloudletHistory.put(vm.getId(), candidate);
					}
				} else {
					break;
				}

			}
		}
		
		ECloudlet selectedCloudlet = null;
		Double vmct = null;
		
		for(Integer vmId : vmCloudletHistory.keySet()) {
			if(vmct == null || newVmCtMap.get(vmId) < vmct) {
				vmct = newVmCtMap.get(vmId);
				selectedCloudlet = vmCloudletHistory.get(vmId);
			}
		}

		return selectedCloudlet;
	}

	private static Double getAverageMakeSpan(Map<Integer, Double> vm_ct) {

		Double sum = 0.0;

		for (Double e : vm_ct.values()) {
			sum += e;
		}

		return sum / vm_ct.size();
	}

	private static HashMap<Integer, Double> getVmMakeSpan(List<ECloudlet> cloudletlist) {
		HashMap<Integer, Double> vm_makespan = new HashMap<>();

		for (ECloudlet cl : cloudletlist) {
			if (cl.getVmId() != -1) {
				if (!vm_makespan.containsKey(cl.getVmId()) || cl.getFinishTime() > vm_makespan.get(cl.getVmId())) {
					vm_makespan.put(cl.getVmId(), cl.getFinishTime());
				}
			}
		}

		return vm_makespan;
	}

	private static EVm getVmWithMaxMakeSpan(Map<Integer, Double> vm_ct, List<EVm> vmlist) {

		EVm max = null;

		for (EVm vm : vmlist) {
			if (Objects.isNull(max) || vm_ct.get(vm.getId()) > vm_ct.get(max.getId())) {
				max = vm;
			}
		}

		return max;

	}
	
	private static EVm getVmWithMinVmCt(Map<Integer, Double> vm_ct, List<EVm> vmlist, ECloudlet selectedCloudlet) {

		EVm min = null;

		for (EVm vm : vmlist) {
			if (Objects.isNull(min) || 
					vm_ct.get(vm.getId()) + (selectedCloudlet.getCloudletLength() / vm.getMips()) <
					vm_ct.get(min.getId()) + (selectedCloudlet.getCloudletLength() / min.getMips())
					) {
				min = vm;
			}
		}

		return min;

	}

	private static EVm getVmWithMinMakeSpan(Map<Integer, Double> vm_ct, List<EVm> vmlist) {

		EVm min = null;

		for (EVm vm : vmlist) {
			if (Objects.isNull(min) || vm_ct.get(vm.getId()) > vm_ct.get(min.getId())) {
				min = vm;
			}
		}

		return min;

	}

	private static Host createHost(int id, int shiftId, int ram, int bw, long storage, List<Pe> peList1,
			List<Pe> peList2) {
		return new Host(id + shiftId, new RamProvisionerSimple(ram), new BwProvisionerSimple(bw), storage, peList1,
				new VmSchedulerTimeShared(peList2));
	}

	private static Datacenter createDatacenter(String name) {

		// Here are the steps needed to create a PowerDatacenter:
		// 1. We need to create a list to store one or more
		// Machines
		List<Host> hostList = new ArrayList<Host>();

		// 2. A Machine contains one or more PEs or CPUs/Cores. Therefore, should
		// create a list to store these PEs before creating
		// a Machine.
		List<Pe> peList1 = new ArrayList<Pe>();

		int mips = 4000;

		// 3. Create PEs and add these into the list.
		// for a quad-core machine, a list of 4 PEs is required:
		peList1.add(new Pe(0, new PeProvisionerSimple(mips))); // need to store Pe id and MIPS Rating
		peList1.add(new Pe(1, new PeProvisionerSimple(mips)));
		peList1.add(new Pe(2, new PeProvisionerSimple(mips)));
		peList1.add(new Pe(3, new PeProvisionerSimple(mips)));

		// Another list, for a dual-core machine
		List<Pe> peList2 = new ArrayList<Pe>();

		peList2.add(new Pe(0, new PeProvisionerSimple(mips)));
		peList2.add(new Pe(1, new PeProvisionerSimple(mips)));

		// 4. Create Hosts with its id and list of PEs and add them to the list of
		// machines
		int hostId = 0;
		int ram = 16384; // host memory (MB)
		long storage = 1000000; // host storage
		int bw = 10000;

		for (; hostId < 4; hostId++) {
			hostList.add(createHost(hostId, 0, ram, bw, storage, peList1, peList2));
		}

		for (; hostId < 26; hostId++) {
			hostList.add(createHost(hostId, 0, ram, bw, storage, peList1, peList1));
		}

		// To create a host with a space-shared allocation policy for PEs to VMs:
		// hostList.add(
		// new Host(
		// hostId,
		// new CpuProvisionerSimple(peList1),
		// new RamProvisionerSimple(ram),
		// new BwProvisionerSimple(bw),
		// storage,
		// new VmSchedulerSpaceShared(peList1)
		// )
		// );

		// To create a host with a oportunistic space-shared allocation policy for PEs
		// to VMs:
		// hostList.add(
		// new Host(
		// hostId,
		// new CpuProvisionerSimple(peList1),
		// new RamProvisionerSimple(ram),
		// new BwProvisionerSimple(bw),
		// storage,
		// new VmSchedulerOportunisticSpaceShared(peList1)
		// )
		// );

		// 5. Create a DatacenterCharacteristics object that stores the
		// properties of a data center: architecture, OS, list of
		// Machines, allocation policy: time- or space-shared, time zone
		// and its price (G$/Pe time unit).
		String arch = "x86"; // system architecture
		String os = "Linux"; // operating system
		String vmm = "Xen";
		double time_zone = 10.0; // time zone this resource located
		double cost = 3.0; // the cost of using processing in this resource
		double costPerMem = 0.05; // the cost of using memory in this resource
		double costPerStorage = 0.1; // the cost of using storage in this resource
		double costPerBw = 0.1; // the cost of using bw in this resource
		LinkedList<Storage> storageList = new LinkedList<Storage>(); // we are not adding SAN devices by now

		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(arch, os, vmm, hostList, time_zone,
				cost, costPerMem, costPerStorage, costPerBw);

		// 6. Finally, we need to create a PowerDatacenter object.
		Datacenter datacenter = null;
		try {
			datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return datacenter;
	}

	// We strongly encourage users to develop their own broker policies, to submit
	// vms and cloudlets according
	// to the specific rules of the simulated scenario
	private static DatacenterBroker createBroker() {

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
	 * 
	 * @param list list of Cloudlets
	 */
	private static void printCloudletList(List<ECloudlet> list) {
		int size = list.size();
		ECloudlet cloudlet;

		String indent = "    ";
		Log.printLine();
		Log.printLine("========== OUTPUT ==========");
		Log.printLine("Cloudlet ID" + indent + "STATUS" + indent + "Data center ID" + indent + "VM ID" + indent + indent
				+ "Time" + indent + "Start Time" + indent + "Finish Time");

		DecimalFormat dft = new DecimalFormat("###.##");
		for (int i = 0; i < size; i++) {
			cloudlet = list.get(i);
			Log.print(indent + cloudlet.getCloudletId() + indent + indent);

			if (cloudlet.getCloudletStatus() == ECloudlet.SUCCESS) {
				Log.print("SUCCESS");

				Log.printLine(indent + indent + cloudlet.getResourceId() + indent + indent + indent + cloudlet.getVmId()
						+ indent + indent + indent + dft.format(cloudlet.getActualCPUTime()) + indent + indent
						+ dft.format(cloudlet.getExecStartTime()) + indent + indent + indent
						+ dft.format(cloudlet.getFinishTime()));
			}
		}

	}

	////////////////////////// STATIC METHODS ///////////////////////

	/**
	 * Creates main() to run this example
	 */
	/**
	 * @param args
	 */
	/**
	 * @param args
	 */
	/**
	 * @param args
	 */
	public static void run(int n) {
		Log.printLine("Starting " + MethodHandles.lookup().lookupClass().getSimpleName() + "...");
		
		COUNT_ASSIGN = 0;
		COUNT_ADJUST = 0;
		COUNT_ACQUIRE = 0;
		COUNT_FORCED = 0;
		
		/** The cloudlet list. */
		List<ECloudlet> cloudletList;

		/** The vmlist. */
		List<EVm> vmList;

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

			List<EVm> availableVmList = createExperimentVM(brokerId);
			vmList = new LinkedList<>();
			int vmsToPreallocate = (int) (availableVmList.size() * PREACQUIRED_VM);
			for (int i = 0; i < vmsToPreallocate; i++) {
				int vmRemoveIndex = genRandom(0, availableVmList.size() - 1);
				vmList.add(availableVmList.remove(vmRemoveIndex));
			}
			if (WORKLOAD == 0)
				cloudletList = createSyntheticCloudlet(brokerId);
			else if (WORKLOAD == 1)
				cloudletList = createGoogleCloudlet(brokerId);
			else {
				cloudletList = createSyntheticCloudlet(brokerId);
				cloudletList.addAll(createGoogleCloudlet(brokerId));
			}

			if (vmList.size() != 0)
				broker.submitVmList(vmList);
			broker.submitCloudletList(cloudletList);

			List<ECloudlet> remainingCloudlet = new ArrayList<>(cloudletList);
			List<ECloudlet> rejectedCloudlet = new ArrayList<>();
			HashMap<Integer, EVm> vmMap = new HashMap<>();

			for (EVm vm : vmList) {
				vmMap.put(vm.getId(), vm);
			}

			Map<Integer, Double> vmctMap = getVmCt(vmList, cloudletList);

			// CODE STARTS HERE

			// TODO Scheduler

			while (remainingCloudlet.size() != 0) {

				/*
				 * Since these are objects, java uses Timsort by default which has complexity
				 * Best Case: n
				 * Average Case: nlogn
				 * Worst Case: nlogn
				 * Memory Complexity: n
				 * Stable: Yes
				 */
				if (SORT_TYPE == 1) {
					// 95-96
					remainingCloudlet
							.sort((o1, o2) -> Double.compare(o1.getDeadlineTime() + 1, o2.getDeadlineTime() + 1));
				} else if (SORT_TYPE == 2) {
					// 98-100
					remainingCloudlet
							.sort((o1, o2) -> Double.compare((o1.getDeadlineTime() + 1) / (o1.getCloudletLength() + 1),
									(o2.getDeadlineTime() + 1) / (o2.getCloudletLength() + 1)));
				} else if (SORT_TYPE == 3) {
					// 93-95
					remainingCloudlet
							.sort((o1, o2) -> Double.compare((o1.getCloudletLength() + 1) / (o1.getDeadlineTime() + 1),
									(o2.getCloudletLength() + 1) / (o2.getDeadlineTime() + 1)));
				}

				ECloudlet selectedCloudlet = remainingCloudlet.remove(0);

				boolean foundVm = false;
				Integer assignedVmId = null;
//				List<Integer> vmctSortedId = sortVmCt(vmctMap, vmMap, selectedCloudlet);
//				EVm mctVM = null;
//				if(vmctSortedId.size() != 0) {
//					mctVM = vmMap.get(vmctSortedId.get(0));
//				}
				EVm mctVM = getVmWithMinVmCt(vmctMap, vmList, selectedCloudlet);
								
				if (mctVM != null) {
					
					EVm vm = mctVM;
					Integer vmId = vm.getId();
					// Deadline Check

					// current_VM_CT + (Cloudlet.length / VM.mips)
					double cloudletCt = selectedCloudlet.getCloudletLength() / vm.getMips();
					double currentVmCt = vmctMap.get(vmId);
					double newVmCt = currentVmCt + cloudletCt;
					double deadline = selectedCloudlet.getDeadlineTime();

					// TODO Initial check
					if (newVmCt <= deadline) {
						foundVm = true;
						assignedVmId = vmId;
						COUNT_ASSIGN = COUNT_ASSIGN + 1;
					}
				}

				if(!foundVm) {
					
					for (int vmi = 0; vmi < vmList.size(); vmi++) {
						
						EVm vm = vmMap.get(vmi);
						Integer vmId = vm.getId();
						// Deadline Check

						// current_VM_CT + (Cloudlet.length / VM.mips)
						double cloudletCt = selectedCloudlet.getCloudletLength() / vmMap.get(vmId).getMips();
						double currentVmCt = vmctMap.get(vmId);
						double newVmCt = currentVmCt + cloudletCt;
						
						// TODO Check Adjust
						// TODO Improve Adjust to handle everything in by the broker.getCloudletList() list 
						// rather than needing to supply the VM each time. This is to improve compute time,
						// from O(Cl.Vm) into O(Cl) 
						ECloudlet otherCloudlet = checkAdjustPossible(selectedCloudlet, broker.getCloudletList(), vm, newVmCt);
	
						// TODO Perorm Adjust
						if (!Objects.isNull(otherCloudlet)) {
	
							broker.getCloudletList().remove(selectedCloudlet);
							broker.getCloudletList().add(broker.getCloudletList().indexOf(otherCloudlet), selectedCloudlet);
	
							foundVm = true;
							assignedVmId = vmId;
							COUNT_ADJUST = COUNT_ADJUST + 1;
							break;
	
						}
	
					}
					
				}

				if (!foundVm) {

					// TODO Find resource
					EVm acquiredVm = checkAvailableVms(availableVmList, selectedCloudlet, FIND_NEW_VM_WITHIN_DEADLINE);
					
					if (!FIND_NEW_VM_WITHIN_DEADLINE && acquiredVm != null) {
						
						double cloudletCt = selectedCloudlet.getCloudletLength() / acquiredVm.getMips();
						double acquiredVmCt = acquiredVm.getAcquisitionDelay() + acquiredVm.getBootTime() + cloudletCt;
						
						// If new VM is slower than already acquired VM, discard it.
						// This check is only useful when we find VM if it cannot finish task within deadline.
						if(mctVM != null && vmctMap.get(mctVM.getId()) + cloudletCt <= acquiredVmCt) {
							acquiredVm = null;
							assignedVmId = mctVM.getId();
							foundVm = true;
							COUNT_FORCED = COUNT_FORCED + 1;
						} else if (acquiredVmCt > selectedCloudlet.getDeadlineTime()) {
							rejectedCloudlet.add(selectedCloudlet);
							COUNT_FORCED = COUNT_FORCED + 1;
						}
						
					}

					// TODO Create resource
					if (acquiredVm != null) {
						List<EVm> newVm = new LinkedList<>();
						newVm.add(acquiredVm);
						availableVmList.remove(acquiredVm);
						broker.submitVmList(newVm);
						vmList.addAll(newVm);
						for (EVm vm : newVm) {
							vmMap.put(vm.getId(), vm);
							vmctMap.put(vm.getId(), vm.getAcquisitionDelay() + vm.getBootTime());
						}
						assignedVmId = newVm.get(0).getId();
						foundVm = true;
						COUNT_ACQUIRE = COUNT_ACQUIRE + 1;
					}

				}

				if (!foundVm) {
					if(ALLOW_REJECTS_TO_RUN == 1) {
						
						// Just in case if we have no VM and we are forced to RUN
						
						if(vmList.size() == 0) {
							EVm acquiredVm = checkAvailableVms(availableVmList, selectedCloudlet, false);
							
							if (acquiredVm != null) {
								List<EVm> newVm = new LinkedList<>();
								newVm.add(acquiredVm);
								availableVmList.remove(acquiredVm);
								broker.submitVmList(newVm);
								vmList.addAll(newVm);
								for (EVm vm : newVm) {
									vmMap.put(vm.getId(), vm);
									vmctMap.put(vm.getId(), vm.getAcquisitionDelay() + vm.getBootTime());
								}
								assignedVmId = newVm.get(0).getId();
								foundVm = true;
								COUNT_ACQUIRE = COUNT_ACQUIRE + 1;
							}
							
						}
						
						if(vmList.size() != 0 && mctVM != null) {
							assignedVmId = mctVM.getId();
							foundVm = true;
							double cloudletCt = selectedCloudlet.getCloudletLength() / vmMap.get(assignedVmId).getMips();
							double newVmCt = vmctMap.get(assignedVmId) + cloudletCt;
							COUNT_FORCED = COUNT_FORCED + 1;
							System.out.println(String.format("R%d\t%f\t%f", selectedCloudlet.getCloudletId(), selectedCloudlet.getDeadlineTime() / selectedCloudlet.getCloudletLength(), newVmCt));
						} else {
							System.out.println(String.format("N%d\t%f\t%f", selectedCloudlet.getCloudletId(), selectedCloudlet.getDeadlineTime() / selectedCloudlet.getCloudletLength(), 0.0));
						}
					}
					
					rejectedCloudlet.add(selectedCloudlet);
					
					if(ALLOW_REJECTS_TO_RUN == 0) {
						cloudletList.remove(selectedCloudlet);
						broker.getCloudletList().remove(selectedCloudlet);
					}
				} 
				
				if(foundVm) {
					double cloudletCt = selectedCloudlet.getCloudletLength() / vmMap.get(assignedVmId).getMips();
					double newVmCt = vmctMap.get(assignedVmId) + cloudletCt;
					System.out.println(String.format("%d\t%f\t%f", selectedCloudlet.getCloudletId(), selectedCloudlet.getDeadlineTime() / selectedCloudlet.getCloudletLength(), newVmCt));
					vmctMap.put(assignedVmId, newVmCt);
					broker.bindCloudletToVm(selectedCloudlet.getCloudletId(), assignedVmId);
				}
				
				
				if(remainingCloudlet.size() == 0) {
					
					// TODO release resource
					
//					if(vmList.size() != 0) {
//						
//						for(Iterator<EVm> iter = vmList.iterator(); iter.hasNext(); ) {
//							EVm vm = iter.next();
//							double finishTime = vmctMap.get(vm.getId()) + vm.getAcquisitionDelay() + vm.getBootTime();
//							if(finishTime < System.currentTimeMillis() / 1000) {
//								iter.remove();
//								vmctMap.remove(vm.getId());
//								vmMap.remove(vm.getId());
//							}
//						}
//						
//					} else {
//						break;
//					}
					
				}

			}

			// CODE ENDS HERE FOR SCHEDULER, RESUMED LATER

			System.out.println("START");
			
			// Fifth step: Starts the simulation
			CloudSim.startSimulation();

			// Final step: Print results when simulation is over
			List<ECloudlet> newList = broker.getCloudletReceivedList();

			CloudSim.stopSimulation();

			printCloudletList(newList);

			// CODE RESUMES HERE

			// Metrics

			Map<Integer, Double> vm_ct = getVmCt(vmList, cloudletList);
			//getVmMakeSpan uses getFinishTime() instead of calculating length/mips, but values are almost the same as getVmCt
			Map<Integer, Double> vm_makespan = getVmMakeSpan(cloudletList);
			EVm vmWitHighestMakeSpan = getVmWithMaxMakeSpan(vm_ct, vmList);
			Double averageMakeSpan = getAverageMakeSpan(vm_ct);
			Double makespan = vm_ct.get(vmWitHighestMakeSpan.getId());
			Double throughput = newList.size() / makespan;
			Double arur = averageMakeSpan / makespan;
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

			System.out.println(String.format("Total: %d", totalTasks));
			System.out.println(String.format("Rejected: %d", rejectedCloudlet.size()));
			System.out.println(String.format("Assigned: %d", COUNT_ASSIGN));
			System.out.println(String.format("Adjusted: %d", COUNT_ADJUST));
			System.out.println(String.format("Acquired: %d", COUNT_ACQUIRE));
			System.out.println(String.format("Forced: %d", COUNT_FORCED));
			
			t_makespan[n] = makespan;
			t_throughput[n] = throughput;
			t_arur[n] = arur;
			t_accept[n] = acceptedRate;
			t_reject[n] = rejectionRate;
			
			t_assign[n] = COUNT_ASSIGN;
			t_adjust[n] = COUNT_ADJUST;
			t_acquired[n] = COUNT_ACQUIRE;
			t_forced[n] = COUNT_FORCED;
			
			// CODE ENDS HERE

			Log.printLine(MethodHandles.lookup().lookupClass().getSimpleName() + " finished!");
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("The simulation has been terminated due to an unexpected error");
		}
	}
	
	public static Double getAvg(Double[] args) {
		Double sum = 0.0;
		for(Double arg: args) {
			sum = sum + arg;
		}
		return sum / args.length;
	}
	
	public static Double getAvg(Integer[] args) {
		Double sum = 0.0;
		for(Integer arg: args) {
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
		
		System.out.println("-------------------------------------");
		
		System.out.println(String.format("Assigned: %f", getAvg(t_assign)));
		System.out.println(String.format("Adjusted: %f", getAvg(t_adjust)));
		System.out.println(String.format("Acquired: %f", getAvg(t_acquired)));
		System.out.println(String.format("Forced: %f", getAvg(t_forced)));
		
		System.out.println("-------------------------------------");
		
		System.out.println(String.format("%f", getAvg(t_makespan)));
		System.out.println(String.format("%f", getAvg(t_throughput)));
		System.out.println(String.format("%f", getAvg(t_arur)));

		System.out.println(String.format("%f", getAvg(t_reject)));
		System.out.println(String.format("%f", getAvg(t_accept)));
		
		System.out.println("-------------------------------------");
		
		System.out.println(String.format("%f", getAvg(t_assign)));
		System.out.println(String.format("%f", getAvg(t_adjust)));
		System.out.println(String.format("%f", getAvg(t_acquired)));
		System.out.println(String.format("%f", getAvg(t_forced)));
		
	}
	
}
