package org.cloudbus.cloudsim.examples;

import java.util.Random;

import org.cloudbus.cloudsim.CloudletScheduler;
import org.cloudbus.cloudsim.Vm;

public class EVm extends Vm {

	private double acquisitionDelay = 0;
	private double bootTime = 0;
	
	private static double BOOT_TIME_MIN = 0.0;
	private static double BOOT_TIME_MAX = 0.0;
	
	private static double ACQUISITION_DELAY_MIN = 0.0;
	private static double ACQUISITION_DELAY_MAX = 0.0;
	
	public EVm(int id, int userId, double mips, int numberOfPes, int ram, long bw, long size, String vmm,
			CloudletScheduler cloudletScheduler, boolean genRandom) {
		super(id, userId, mips, numberOfPes, ram, bw, size, vmm, cloudletScheduler);
		if(genRandom) {
			this.bootTime = genRandomDouble(BOOT_TIME_MIN, BOOT_TIME_MAX);
			this.acquisitionDelay = genRandomDouble(ACQUISITION_DELAY_MIN, ACQUISITION_DELAY_MAX);
		}
	}
	
	public EVm(int id, int userId, double mips, int numberOfPes, int ram, long bw, long size, String vmm,
			CloudletScheduler cloudletScheduler) {
		super(id, userId, mips, numberOfPes, ram, bw, size, vmm, cloudletScheduler);
	}
	
	public EVm(int id, int userId, double mips, int numberOfPes, int ram, long bw, long size, String vmm,
			CloudletScheduler cloudletScheduler, double boot_time, double acquisition_delay) {
		super(id, userId, mips, numberOfPes, ram, bw, size, vmm, cloudletScheduler);
		this.setBootTime(boot_time);
		this.setAcquisitionDelay(acquisition_delay);
	}
	
	public static Double genRandomDouble(double low, double high) {
		return low + (high - low) * new Random().nextDouble();
	}

	public double getBootTime() {
		return bootTime;
	}

	public void setBootTime(double boot_time) {
		this.bootTime = boot_time;
	}

	public double getAcquisitionDelay() {
		return acquisitionDelay;
	}

	public void setAcquisitionDelay(double acquisition_delay) {
		this.acquisitionDelay = acquisition_delay;
	}
	
	public static double getAverageBootTime() {
		return BOOT_TIME_MIN + BOOT_TIME_MAX / 2;
	}
	
	public static double getAverageAcquisitionDelay() {
		return ACQUISITION_DELAY_MIN + ACQUISITION_DELAY_MAX / 2;
	}

}
