package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.UtilizationModel;

public class ECloudlet extends Cloudlet {
	
	private double arrivalTime = 0;
	private double deadlineTime = 0;
	private boolean duplicate = false;
	private int previousAssignedVmId = -1;

	public ECloudlet(int cloudletId, long cloudletLength, int pesNumber, long cloudletFileSize, long cloudletOutputSize,
			UtilizationModel utilizationModelCpu, UtilizationModel utilizationModelRam,
			UtilizationModel utilizationModelBw) {
		super(cloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize, utilizationModelCpu,
				utilizationModelRam, utilizationModelBw);
	}
	
	public ECloudlet(int cloudletId, long cloudletLength, int pesNumber, long cloudletFileSize, long cloudletOutputSize,
			UtilizationModel utilizationModelCpu, UtilizationModel utilizationModelRam,
			UtilizationModel utilizationModelBw, double arrivalTime, double deadlineTime) {
		super(cloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize, utilizationModelCpu,
				utilizationModelRam, utilizationModelBw);
		this.arrivalTime = arrivalTime;
		this.deadlineTime = this.arrivalTime + deadlineTime;
	}
	
	public ECloudlet(Cloudlet cloudlet, double arrivalTime, double deadlineTime) {
		super(cloudlet.getCloudletId(), cloudlet.getCloudletLength(), cloudlet.getNumberOfPes(), cloudlet.getCloudletFileSize(), cloudlet.getCloudletOutputSize(), 
				cloudlet.getUtilizationModelCpu(), cloudlet.getUtilizationModelRam(), cloudlet.getUtilizationModelBw());
		this.arrivalTime = arrivalTime;
		this.deadlineTime = this.arrivalTime + deadlineTime;
	}
	
	public double getArrivalTime() {
		return arrivalTime;
	}

	public void setArrivalTime(double arrivalTime) {
		this.arrivalTime = arrivalTime;
	}

	public double getDeadlineTime() {
		return deadlineTime;
	}

	public void setDeadlineTime(double deadlineTime) {
		this.deadlineTime = this.arrivalTime + deadlineTime;
	}
	
	public int getPreviousAssignedVmId() {
		return this.previousAssignedVmId;
	}
	
	public void setPreviousAssignedVmId(int previousAssignedVmId) {
		this.previousAssignedVmId = previousAssignedVmId;
		this.setDuplicate(true);
	}

	public boolean isDuplicate() {
		return duplicate;
	}

	public void setDuplicate(boolean isDuplicate) {
		this.duplicate = isDuplicate;
	}

}
