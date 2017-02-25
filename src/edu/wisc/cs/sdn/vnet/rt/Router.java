package edu.wisc.cs.sdn.vnet.rt;

import java.nio.ByteBuffer;
import java.util.Map.Entry;

import edu.wisc.cs.sdn.vnet.Device;
import edu.wisc.cs.sdn.vnet.DumpFile;
import edu.wisc.cs.sdn.vnet.Iface;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;

/**
 * @author Aaron Gember-Jacobson and Anubhavnidhi Abhashkumar
 */
public class Router extends Device
{	
	private final boolean DEBUG = true;
	
	/** Routing table for the router */
	private RouteTable routeTable;

	/** ARP cache for the router */
	private ArpCache arpCache;

	/**
	 * Creates a router for a specific host.
	 * @param host hostname for the router
	 */
	public Router(String host, DumpFile logfile)
	{
		super(host,logfile);
		this.routeTable = new RouteTable();
		this.arpCache = new ArpCache();
	}

	/**
	 * @return routing table for the router
	 */
	public RouteTable getRouteTable()
	{ return this.routeTable; }

	/**
	 * Load a new routing table from a file.
	 * @param routeTableFile the name of the file containing the routing table
	 */
	public void loadRouteTable(String routeTableFile)
	{
		if (!routeTable.load(routeTableFile, this))
		{
			System.err.println("Error setting up routing table from file "
					+ routeTableFile);
			System.exit(1);
		}

		System.out.println("Loaded static route table");
		System.out.println("-------------------------------------------------");
		System.out.print(this.routeTable.toString());
		System.out.println("-------------------------------------------------");
	}

	/**
	 * Load a new ARP cache from a file.
	 * @param arpCacheFile the name of the file containing the ARP cache
	 */
	public void loadArpCache(String arpCacheFile)
	{
		if (!arpCache.load(arpCacheFile))
		{
			System.err.println("Error setting up ARP cache from file "
					+ arpCacheFile);
			System.exit(1);
		}

		System.out.println("Loaded static ARP cache");
		System.out.println("----------------------------------");
		System.out.print(this.arpCache.toString());
		System.out.println("----------------------------------");
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
		/* TODO: Handle packets                                             */

		// 1. Check the incoming packet type
		if(etherPacket.getEtherType() != Ethernet.TYPE_IPv4){
			// if the incoming packet type is not ipv4
			// drop packet
			if(DEBUG) System.out.println("Packet Type is not IPv4");
			return;
		}

		IPv4 packet = (IPv4) etherPacket.getPayload();

		// 2. Check the packet checksum
		short checksum = packet.getChecksum();

		byte[] data = packet.serialize();
		ByteBuffer bb = ByteBuffer.wrap(data);

		bb.putShort(10,(short) 0x0000);

		// calculate checksum
		int accumulation = 0;
		for (int i = 0; i < packet.getHeaderLength() * 2; ++i) {
			accumulation += 0xffff & bb.getShort();
		}
		accumulation = ((accumulation >> 16) & 0xffff)
				+ (accumulation & 0xffff);

		short checksumCalcd = (short)(~accumulation & 0xffff);

		if(checksum != checksumCalcd){
			// if calculated checksum is different from received one
			// drop packet
			if(DEBUG) System.out.println("Checksum is not matched (ori:"+checksum+", calcd:"+checksumCalcd+")");
			return;
		}

		// 3. Decrease and Check TTL
		byte ttl = packet.getTtl();
		ttl--;

		if(ttl==0){
			// if ttl becomes 0
			// drop packet
			if(DEBUG) System.out.println("TTL expired");
			return;
		}
		packet.setTtl(ttl);

		// 4. Check interfaces that has dest IP.
		for(Entry<String, Iface> entry : this.interfaces.entrySet()){
			Iface iface = entry.getValue();
			if(iface.getIpAddress() == packet.getDestinationAddress()){
				// if router has interface for dest ip
				// drop packet
				if(DEBUG) System.out.println("Destination interface IP is found on router");
				return;
			}
		}

		// 5. lookup route table
		RouteEntry routeEntry = this.getRouteTable().lookup(packet.getDestinationAddress());
		if(routeEntry == null){
			// if no route entry matches
			// drop packet
			if(DEBUG) System.out.println("Route entry to dest addr not found");
			return;
		}

		// 6. lookup ARP cache and change the packet's dest mac addr
		ArpEntry arpEntry = this.arpCache.lookup(packet.getDestinationAddress());
		if(arpEntry == null){
			// if no arp entry matches
			// drop packet
			if(DEBUG) System.out.println("ARP entry to dest addr not found");
		}
		etherPacket.setDestinationMACAddress(arpEntry.getMac().toBytes());
		etherPacket.setSourceMACAddress(routeEntry.getInterface().getMacAddress().toBytes());

		// 7. update checksum
		// if you set your checksum to 0, serialize method wiil calculate the checksum value.
		packet.resetChecksum();
		packet.serialize(); 

		// 8. send packet
		sendPacket(etherPacket, routeEntry.getInterface());

		/********************************************************************/
	}
}
