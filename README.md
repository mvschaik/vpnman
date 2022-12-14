# VPN Manager

This project provides a simple web interface to configure a Mikrotik router to connect to NordVPN and configures policy
routing to route a couple of devices through the VPN.

This is a very specific use case, and only tested with my own devices.

## Router configuration

The tool attempts to configure the router as much as possible:

1. Configure Wireguard interface and IP address for NordLynx:
   ```
   /interface/wireguard add name=nordlynx1 listen-port=51820 private-key=XXX
   /ip/address add interface=nordlynx1 address=10.5.0.2/29
   /ip/firewall/nat add chain=srcnat action=masquerade out-interface=nordlynx1
   ```

2. Fetch a list of servers from NordVPN and configures a Wireguard peer:
   ```
   /interface/wireguard/peers add interface=nordlynx1 endpoint-address=w.x.y.z endpoint-port=51820 public-key=XXX allowed-address=0.0.0.0/0
   ```

3. Add a routing table for routing traffic to the nordlynx1 interface.
   ```
   /routing/table add fib name=to-vpn
   /ip/route add dst-address=0.0.0.0/0 gateway=10.5.0.1 routing-table=to-vpn
   ```

4. Configure routing rules for each local host to pass them through the to-vpn table.
   ```
   /routing/rule add src-address=w.x.y.z action=lookup table=to-vpn
   ```

Configuration can be pre-configured and will be kept intact as much as possible.

## Running

You might need to configure https API access and a user on your Mikrotik device.

You can provide your private key for NordVPN/Nordlynx as a variable, or preconfigure a wireguard interface called
"nordlynx1" on your router. (There's
various [articles](https://forum.gl-inet.com/t/configure-wireguard-client-to-connect-to-nordvpn-servers/10422/25) that
describe how to obtain the key.)

To just run the application:

```sh
routeros.baseUrl=https://<your router address here>/
routeros.username=<your username here>
routeros.password=<your password here>
preferredCountries=nl,gb,us  # Optional
privateWireguardKey=<private key>  # Optional
./gradlew bootRun
```

Or to run in docker:

```sh
./gradlew bootBuildImage --imageName=vpnman
docker run --rm --expose=8080 \
           -e "routeros.baseUrl=https://<your router address>/" \
           -e "routeros.username=<username>" \
           -e "routeros.password=<password>" \
           -it vpnman
```

### TODO

* Add tests. :-)
* Route DNS to NordVPN.
* Store country in browser state.
* Preselect/mark obvious candidate devices, e.g. Chromecast, AppleTV, etc. based on MAC address.

