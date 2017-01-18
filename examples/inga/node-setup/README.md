Set up INGA
===

Use this to setup nodes with default values for node id, pan id, channel and tx power.
Settings will be written to EEPROM when the program starts.

usage: make [target] [MOTES=<device>] [options]
Targets:
- setup(default): Setup with given values
- delete:  Delete all settings

Options:
- EUI64=<value>           default calculated by inga_tool from FTDI serial
- PAN_ADDR=<value>        default last 2 bytes of EUI64
- PAN_ID=<value>          default=0xABCD
- RADIO_CHANNEL=<value>   default=26
- RADIO_TX_POWER=<value>  default=0

Example
---

If only one node is connected to your computer, you can run

    make setup

If multiple nodes are connected, you have to specify which node to set

    make MOTES=/dev/ttyUSBx setup

To check your settings instantly after programming, you can add the login target

    make MOTES=/dev/ttyUSBx setup login

