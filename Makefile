JAVA ?= java
JAVAC ?= javac
JAVAC_ARGS ?= -Xlint:unchecked

PRGM := Server
PKG := ca/dioo/java/SurveillanceServer
ROOT_DIR := $(dir $(lastword $(MAKEFILE_LIST)))
BUILD_DIR := $(ROOT_DIR)/build
BPATH := $(BUILD_DIR)/$(PKG)
empty :=
space := $(empty) $(empty)

src := $(wildcard src/*.java)
objects := $(patsubst src/%.java,$(BPATH)/%.class,$(src))

libs = $(wildcard libs/*.jar)


run:


.PHONY: all
all: $(objects) $(libs)


$(objects): $(BPATH)/%.class: src/%.java $(BUILD_DIR)
	$(JAVAC) $(JAVAC_ARGS) -cp $(subst $(space),:,$(libs)):$(BUILD_DIR) -d $(BUILD_DIR) $<


.PHONY: run
run: all
	$(JAVA) -cp $(subst $(space),:,$(libs)):$(BUILD_DIR) $(subst /,.,$(PKG)/$(PRGM)) $(ARGS)


$(BUILD_DIR):
	@[ -d $(BUILD_DIR) ] || mkdir -p $(BUILD_DIR)


.PHONY: clean
clean:
	@[ ! -e $(BUILD_DIR) ] || rm -rv $(BUILD_DIR)


libs:
	@[ -d libs ] || mkdir libs


.PHONY: libjars
libjars: libs $(libs)


.PHONY: libs/monitor-lib.jar
libs/monitor-lib.jar: libs
	@cd ../monitor-lib/ && $(MAKE) jar


libs/java-getopt.jar: libs
	@cd ../java-getopt/ && $(MAKE) java-getopt.jar


$(BPATH)/DisplayThread.class: $(patsubst %,$(BPATH)/%.class,MessageProvider)
$(BPATH)/MessageProvider.class: $(patsubst %,$(BPATH)/%.class,Message)
$(BPATH)/ServerThread.class: $(patsubst %,$(BPATH)/%.class,ControlThread)
$(BPATH)/Server.class: $(patsubst %,$(BPATH)/%.class,ControlThread ServerThread)
