JAVA ?= java
JAVA_ARGS ?= #-agentlib:jdwp=transport=dt_socket,server=y,suspend=n
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

test_src := $(wildcard test/*.java)
test_objects := $(patsubst test/%.java,$(BUILD_DIR)/%.class,$(test_src))
test_libs := $(wildcard test/libs/*.jar)


run:


.PHONY: all
all: $(objects)


$(objects): $(BPATH)/%.class: src/%.java $(libs) $(BUILD_DIR)
	$(JAVAC) $(JAVAC_ARGS) -cp libs/*:$(BUILD_DIR) -d $(BUILD_DIR) $<


.PHONY: run
run: $(objects)
	$(JAVA) $(JAVA_ARGS) -cp libs/*:$(BUILD_DIR) $(subst /,.,$(PKG)/$(PRGM)) $(ARGS)


.PHONY: test
test: $(test_objects)
	$(JAVA) -ea $(JAVA_ARGS) -cp libs/*:test/libs/*:$(BUILD_DIR) Test


$(test_objects): $(BUILD_DIR)/%.class: test/%.java $(libs) $(BUILD_DIR)
	$(JAVAC) $(JAVAC_ARGS) -cp libs/*:$(BUILD_DIR) -d $(BUILD_DIR) $<



$(BUILD_DIR):
	@[ -d $(BUILD_DIR) ] || mkdir -p $(BUILD_DIR)


.PHONY: clean
clean:
	@[ ! -e $(BUILD_DIR) ] || rm -rv $(BUILD_DIR)


libs:
	@[ -d libs ] || mkdir libs


.PHONY: libjars
libjars: libs $(libs)


$(libs) $(test_libs):
	$(MAKE) -C $(dir $(shell readlink $@)) jar


#Circular dependencies
$(patsubst %,$(BPATH)/%.class,ServerThread ControlThread): $(patsubst %,src/%.java,ServerThread ControlThread)
	$(JAVAC) $(JAVAC_ARGS) -cp libs/*:$(BUILD_DIR) -d $(BUILD_DIR) $(patsubst %,src/%.java,ServerThread ControlThread)

$(BPATH)/Utils.class: $(patsubst %,$(BPATH)/%.class,Config)
$(BPATH)/ItemQueue.class: $(patsubst %,$(BPATH)/%.class,Item)
$(BPATH)/DisplayThread.class: $(patsubst %,$(BPATH)/%.class,ItemQueue)
$(BPATH)/ControlThread.class: $(patsubst %,$(BPATH)/%.class,ItemQueue)
$(BPATH)/ServerThread.class: $(patsubst %,$(BPATH)/%.class,Utils)
$(BPATH)/Server.class: $(patsubst %,$(BPATH)/%.class,ControlThread ServerThread Utils)
