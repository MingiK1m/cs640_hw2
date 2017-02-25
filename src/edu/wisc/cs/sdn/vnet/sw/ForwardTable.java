package edu.wisc.cs.sdn.vnet.sw;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.wisc.cs.sdn.vnet.Iface;
import net.floodlightcontroller.packet.MACAddress;

public class ForwardTable {
	private final boolean DEBUG = true;
	
	private final long TIMEOUT_MS = 15*1000; /* 15 sec */
	private Map<MACAddress, TableItem> forwardTableMap;

	public ForwardTable(){
		forwardTableMap = new HashMap<MACAddress, TableItem>();
	}

	public Iface lookUpForwardTable(MACAddress destMACAddress){
		if(forwardTableMap.containsKey(destMACAddress)){
			TableItem entry = forwardTableMap.get(destMACAddress);
			return entry.getInterface();
		}

		return null;
	}

	public void addForwardEntry(MACAddress destMACAddress, Iface nextHop){
		if(forwardTableMap.containsKey(destMACAddress)){
			forwardTableMap.remove(destMACAddress);
		}
		TableItem newEntry = new TableItem(nextHop);
		forwardTableMap.put(destMACAddress, newEntry);
	}

	public synchronized void updateTable(){
		long curTime = System.currentTimeMillis();

		Iterator<MACAddress> keysetIter = forwardTableMap.keySet().iterator();

		while(keysetIter.hasNext()){
			MACAddress macAddr = keysetIter.next();
			TableItem entry = forwardTableMap.get(macAddr);

			if(curTime-entry.getTimestamp() > TIMEOUT_MS){
				if(DEBUG) System.out.println(macAddr.toString() + " : removed from forwarding table for timeout");
				keysetIter.remove();
			}
		}
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
