package edu.wisc.cs.sdn.vnet.sw;

import java.util.HashMap;
import java.util.Map;

import edu.wisc.cs.sdn.vnet.Iface;
import net.floodlightcontroller.packet.MACAddress;

public class ForwardTable {
	private final long MS_15SEC = 15*1000;
	private Map<MACAddress, TableItem> forwardTableMap;
	
	public ForwardTable(){
		forwardTableMap = new HashMap<MACAddress, TableItem>();
	}
	
	public Iface lookUpForwardTable(MACAddress destMACAddress){
		if(forwardTableMap.containsKey(destMACAddress)){
			TableItem entry = forwardTableMap.get(destMACAddress);
			if(System.currentTimeMillis()-entry.getTimestamp() > MS_15SEC){
				forwardTableMap.remove(destMACAddress);
				return null;
			}
			else {
				return entry.getInterface();
			}
		}
		
		return null;
	}
	
	public void addForwardEntry(MACAddress destMACAddress, Iface next_hop){
		if(forwardTableMap.containsKey(destMACAddress)){
			forwardTableMap.remove(destMACAddress);
		}
		TableItem newEntry = new TableItem(next_hop);
		forwardTableMap.put(destMACAddress, newEntry);
	}
	
	private class TableItem{
		private Iface next_hop;
		private long timestamp;
		
		public TableItem(Iface next){
			this.next_hop = next;
			this.timestamp = System.currentTimeMillis();
		}
		
		public long getTimestamp(){
			return this.timestamp;
		}
		
		public Iface getInterface(){
			return this.next_hop;
		}
	}
}
