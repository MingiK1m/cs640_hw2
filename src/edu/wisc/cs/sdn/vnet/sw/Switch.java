package edu.wisc.cs.sdn.vnet.sw;

import java.util.Iterator;

import edu.wisc.cs.sdn.vnet.Device;
import edu.wisc.cs.sdn.vnet.DumpFile;
import edu.wisc.cs.sdn.vnet.Iface;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.MACAddress;

/**
 * @author Aaron Gember-Jacobson
 */
public class Switch extends Device
{	
	private ForwardTable forward_table;
	/**
	 * Creates a router for a specific host.
	 * @param host hostname for the router
	 */
	public Switch(String host, DumpFile logfile)
	{
		super(host,logfile);
		forward_table = new ForwardTable();
	}

	/**
	 * Handle an Ethernet packet received on a specific interface.
	 * @param etherPacket the Ethernet packet that was received
	 * @param inIface the interface on which the packet was received
	 */
	public void handlePacket(Ethernet etherPacket, Iface inIface)
	{
		System.out.println("*** -> Received packet: " +
                etherPacket.toString().replace("\n", "\n\t"));
		
		/********************************************************************/
		// 1. update table
		MACAddress srcMac = etherPacket.getSourceMAC();
		forward_table.addForwardEntry(srcMac, inIface);
		
		// 2. find interface
		MACAddress destMac = etherPacket.getDestinationMAC();
		Iface outIface = forward_table.lookUpForwardTable(destMac);
		
		// 3. send packets
		if(outIface == null){
			// couldn't find table entry or time out.
			// send the packet to all ifaces.
			Iterator<String> keysetIter = getInterfaces().keySet().iterator();
			
			while(keysetIter.hasNext()){
				String name = keysetIter.next();
				Iface iface = interfaces.get(name);
				
				if(iface.getName() == inIface.getName()) 
					continue; // don't send the packet back to where it came from
				
				sendPacket(etherPacket, iface);
			}
		} 
		else {
			// found entry
			// send the packet to iface found on the table.
			sendPacket(etherPacket, outIface);
		}
		/********************************************************************/
	}
}
