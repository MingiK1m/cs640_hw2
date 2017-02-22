package edu.wisc.cs.sdn.vnet.sw;

import java.util.Iterator;
import java.util.Map;

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
		MACAddress src_mac = etherPacket.getSourceMAC();
		forward_table.addForwardEntry(src_mac, inIface);
		
		// 2. find interface
		MACAddress dest_mac = etherPacket.getDestinationMAC();
		Iface out_iface = forward_table.lookUpForwardTable(dest_mac);
		
		// 3. send packets
		if(out_iface == null){
			// couldn't find table entry or time out.
			// send the packet to all ifaces.
			Iterator<String> keyset_iter = getInterfaces().keySet().iterator();
			
			while(keyset_iter.hasNext()){
				String name = keyset_iter.next();
				Iface iface = interfaces.get(name);
				
				sendPacket(etherPacket, iface);
			}
		} 
		else {
			// found entry
			// send the packet to iface found on the table.
			sendPacket(etherPacket, out_iface);
		}
		/********************************************************************/
	}
}
