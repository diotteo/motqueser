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
all: $(objects)


$(objects): $(BPATH)/%.class: src/%.java $(libs) $(BUILD_DIR)
	$(JAVAC) $(JAVAC_ARGS) -cp $(subst $(space),:,$(libs)):$(BUILD_DIR) -d $(BUILD_DIR) $<


.PHONY: run
run: $(objects)
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


libs/monitor-lib.jar: libs
	@$(MAKE) -C ../monitor-lib/ monitor-lib.jar


libs/java-getopt.jar: libs
	@$(MAKE) -C ../java-getopt/ java-getopt.jar


$(BPATH)/MessageProvider.class: $(patsubst %,$(BPATH)/%.class,Message)
$(BPATH)/DisplayThread.class: $(patsubst %,$(BPATH)/%.class,MessageProvider)
$(BPATH)/ControlThread.class: $(patsubst %,$(BPATH)/%.class,MessageProvider)
$(BPATH)/ServerThread.class: $(patsubst %,$(BPATH)/%.class,ControlThread)
$(BPATH)/Server.class: $(patsubst %,$(BPATH)/%.class,ControlThread ServerThread)
