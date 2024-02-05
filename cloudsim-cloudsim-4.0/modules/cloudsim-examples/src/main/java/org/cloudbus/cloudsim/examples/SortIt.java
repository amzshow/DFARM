package org.cloudbus.cloudsim.examples;

public class SortIt {
	
	public int[] subsort(int[] arr, int index) {
		if(arr[index] > arr[index + 1]) {
			int temp = arr[index];
			arr[index] = arr[index+1];
			arr[index+1] = temp;
		}
		if(arr[index + 1] > arr[index + 2]) {
			int temp = arr[index+1];
			arr[index+1] = arr[index+2];
			arr[index+2] = temp;
		}
		if(arr[index] > arr[index + 1]) {
			int temp = arr[index];
			arr[index] = arr[index+1];
			arr[index+1] = temp;
		}
		return arr;
	}
	
	public int[] sort(int[] arr, int start, int len) {
		
		if(arr != null) {
			if(len == 2) {
				if(arr[0] > arr[1]) {
					int temp = arr[0];
					arr[0] = arr[1];
					arr[1] = temp;
				}
			} else if(len > 2) {
				
				for(int i = start; i < len - 2; i++) {
					arr = subsort(arr, i);
				}
				
				for(int i = len - 3; i >= start; i--) {
					arr = subsort(arr, i);
				}
				
				if(len - 4 > 2) {
					
				}
				
			}
		}
		
		return arr;
	}
	
}
