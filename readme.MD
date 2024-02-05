The source code for the scheduler DFARM along with Ralba and others. This was coded using the Eclipse IDE and the main scheduler class in located in [cloudsim-cloudsim-4.0/modules/cloudsim-examples/src/main/java/org/cloudbus/cloudsim/examples/MyScheduler.java](cloudsim-cloudsim-4.0/modules/cloudsim-examples/src/main/java/org/cloudbus/cloudsim/examples/MyScheduler.java). The other schedulers are located alongside MyScheduler in [RALBA.java](cloudsim-cloudsim-4.0/modules/cloudsim-examples/src/main/java/org/cloudbus/cloudsim/examples/RALBA.java). Both files have controls at the top, they are used to select scheduler and other properties.

# DFARM Code

As mentioned before, the code to the scheduler can be found in [cloudsim-cloudsim-4.0/modules/cloudsim-examples/src/main/java/org/cloudbus/cloudsim/examples/MyScheduler.java](cloudsim-cloudsim-4.0/modules/cloudsim-examples/src/main/java/org/cloudbus/cloudsim/examples/MyScheduler.java)

# Results

Following are the results for the DFARM scheduler campared to other schedulers.

## Synthetic

Following is the results of a simulation for synthetic dataset.

![Synthetic dataset Makespan](images/synthetic/Makespan.svg)
![Synthetic dataset Throughput](images/synthetic/Throughput.svg)
![Synthetic dataset ARUR](images/synthetic/ARUR.svg)
![Synthetic dataset Rejection Rate](images/synthetic/Rejection.svg)

## Google

Following is the results of a simulation for Google-like dataset.

![Google dataset Makespan](images/google/Makespan.svg)
![Google dataset Throughput](images/google/Throughput.svg)
![Google dataset ARUR](images/google/ARUR.svg)
![Google dataset Rejection Rate](images/google/Rejection.svg)
