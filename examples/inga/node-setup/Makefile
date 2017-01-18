CONTIKI_PROJECT = inga-setup

TARGET=inga

APPS += settings_set
APPS += settings_delete

# Preset default values
PAN_ADDR =
PAN_ID = 0xABCD
RADIO_CHANNEL = 26
RADIO_TX_POWER = 0

CONTIKI = ../../..

all: setup

setup: SETTINGS_SET_LOAD = 1
setup: SETTINGS_DELETE_LOAD = 0
setup: test-single $(CONTIKI_PROJECT).upload

setup.jtag: SETTINGS_SET_LOAD = 1
setup.jtag: SETTINGS_DELETE_LOAD = 0
setup.jtag: test-single $(CONTIKI_PROJECT).jtag

setup.bang: SETTINGS_SET_LOAD = 1
setup.bang: SETTINGS_DELETE_LOAD = 0
setup.bang: test-single $(CONTIKI_PROJECT).bang

delete: SETTINGS_SET_LOAD = 0
delete: SETTINGS_DELETE_LOAD = 1
delete: test-single $(CONTIKI_PROJECT).upload

delete.jtag: SETTINGS_SET_LOAD = 0
delete.jtag: SETTINGS_DELETE_LOAD = 1
delete.jtag: test-single $(CONTIKI_PROJECT).jtag

delete.bang: SETTINGS_SET_LOAD = 0
delete.bang: SETTINGS_DELETE_LOAD = 1
delete.bang: test-single $(CONTIKI_PROJECT).bang

# Note: currently, deleting single items is not fully supported by the settings library!
#delete: #TODO

# tests for single MOTE
test-single: NR_OF_MOTES = $(shell echo $(MOTES) | wc -w)
test-single:
	@test ! $(NR_OF_MOTES) -eq 0 || (echo "*** At least one MOTE required. Specify with MOTES=<device>" && false)
	@test $(NR_OF_MOTES) -eq 1 || (echo "*** Only single MOTE allowed. Specify with MOTES=<device>" && false)

include $(CONTIKI)/Makefile.include

# Check environment values and set to defined default if not set
ifndef EUI64
  $(info [$(CONTIKI_PROJECT)] EUI64 is undefined. Trying to calculate one with inga_tool)
  ifneq ($(MOTES),)
    EUI64_FROM_USB = $(shell $(INGA_TOOL) -e -d $(MOTES) | cut -d":" -f2- | sed -e 's/^[ \t]*//')
    ifneq ($(EUI64_FROM_USB),)
      EUI64 ?= $(EUI64_FROM_USB)
      CONF_EUI64 := $(shell echo $(EUI64) | sed 's/\([0-9a-fA-F]\{2\}\)/0x\1/g' | sed 's/:/,/g')
      CFLAGS += -DINGA_CONF_EUI64=$(CONF_EUI64)
      $(info [$(CONTIKI_PROJECT)] EUI64 will be set to $(EUI64))
    else
      $(error [$(CONTIKI_PROJECT)] EUI64 calculation failed.)
    endif
  endif
endif

ifeq ($(PAN_ADDR),)
  $(info [$(CONTIKI_PROJECT)] PAN ADDR is empty. Using last 2 bytes of EUI64)
  PAN_ADDR_FROM_EUI = 0x$(shell echo $(EUI64) | cut -d":" -f7,8 | sed -e 's/://g')
  ifneq ($(PAN_ADDR_FROM_EUI),)
    PAN_ADDR = $(PAN_ADDR_FROM_EUI)
    CFLAGS += -DINGA_CONF_PAN_ADDR=$(PAN_ADDR)
    $(info [$(CONTIKI_PROJECT)] PAN ADDR will be set to $(PAN_ADDR))
  endif
endif

