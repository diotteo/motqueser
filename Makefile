GIT_TAG :=$(shell git describe --tags 2>/dev/null)
ifeq ($(GIT_TAG),)
	VERSION := $(shell git log --pretty=format:%h -n 1)
else
	VERSION := $(GIT_TAG)
endif

JAVA ?= java
JAVA_ARGS ?= #-agentlib:jdwp=transport=dt_socket,server=y,suspend=n
JAVAC ?= javac
JAVAC_ARGS ?= -Xlint:unchecked

PRGM := motqueser
PKG := ca/dioo/java/motqueser
ROOT_DIR := $(dir $(lastword $(MAKEFILE_LIST)))
BUILD_DIR := $(ROOT_DIR)/build
BSRC_DIR := $(BUILD_DIR)/src
JAR_DIR := $(BUILD_DIR)/jar
BPATH := $(JAR_DIR)/$(PKG)
empty :=
space := $(empty) $(empty)

m4_src := $(wildcard src/*.java.m4)
src := $(patsubst src/%.m4,$(BSRC_DIR)/%,$(m4_src))
objects := $(patsubst $(BSRC_DIR)/%.java,$(BPATH)/%.class,$(src))
libs = $(wildcard libs/*.jar)

test_src := $(wildcard test/*.java)
test_objects := $(patsubst test/%.java,$(BUILD_DIR)/%.class,$(test_src))
test_libs := $(wildcard test/libs/*.jar)


all:


.PHONY: dist
dist: jar
	@[ -d dist/$(PRGM) ] || mkdir -p dist/$(PRGM)
	cp $(PRGM).jar libs/*.jar dist/$(PRGM)/
	cp $(PRGM).sh dist/$(PRGM)/
	cp $(PRGM).conf.sample dist/$(PRGM)/
	cd dist/ && tar -cf $(PRGM)-$(VERSION).tar $(PRGM)/
	cd dist/ && bzip2 -f $(PRGM)-$(VERSION).tar
	mv dist/$(PRGM)-$(VERSION).tar.bz2 $(ROOT_DIR)
	rm -rv dist


.PHONY: jar
jar: $(PRGM).jar


$(PRGM).jar: $(objects)
	jar -cf $@ -C $(JAR_DIR) .


.PHONY: all
all: $(objects)


.PHONY: run
run: $(objects)
	$(JAVA) $(JAVA_ARGS) -cp libs/*:$(JAR_DIR) $(subst /,.,$(PKG)/$(PRGM)) $(ARGS)


.PHONY: test
test: $(test_objects)
	$(JAVA) -ea $(JAVA_ARGS) -cp libs/*:test/libs/*:$(BUILD_DIR):$(JAR_DIR) Test


$(test_objects): $(BUILD_DIR)/%.class: test/%.java $(libs) $(BUILD_DIR)
	$(JAVAC) $(JAVAC_ARGS) -cp libs/*:$(BUILD_DIR) -d $(BUILD_DIR) $<



$(BSRC_DIR): $(BUILD_DIR)
$(JAR_DIR): $(BUILD_DIR)
$(BUILD_DIR) $(JAR_DIR) $(BSRC_DIR):
	@[ -d $@ ] || mkdir -p $@


.PHONY: clean
clean:
	@[ ! -e $(BUILD_DIR) ] || rm -rv $(BUILD_DIR)
	@[ ! -e dist ] || rm -rv dist
	@[ ! -e $(PRGM).jar ] || rm -rv $(PRGM).jar


libs:
	@[ -d libs ] || mkdir libs


.PHONY: libjars
libjars: libs $(libs)


$(libs) $(test_libs):
	$(MAKE) -C $(dir $(shell readlink $@)) jar


$(src): $(BSRC_DIR)/%: src/%.m4 $(BSRC_DIR)
	m4 -E -P -DM4_VERSION_MACRO=$(VERSION) $< > $@


#Circular dependencies
item_objects := $(patsubst %,$(BPATH)/%.class,Item Utils)
objects := $(filter-out $(item_objects),$(objects))

$(item_objects) : $(patsubst %,$(BSRC_DIR)/%.java,Item Utils)
	$(JAVAC) $(JAVAC_ARGS) -cp libs/*:$(JAR_DIR) -d $(JAR_DIR) $(patsubst %,$(BSRC_DIR)/%.java,Item Utils)


$(objects): $(BPATH)/%.class: $(BSRC_DIR)/%.java $(libs) $(JAR_DIR)
	$(JAVAC) $(JAVAC_ARGS) -cp libs/*:$(JAR_DIR) -d $(JAR_DIR) $<


$(item_objects): $(patsubst %,$(BPATH)/%.class,Config)

$(BPATH)/ServerThread.class: $(patsubst %,$(BPATH)/%.class,ItemQueue Utils ItemNotFoundException)
$(BPATH)/ScriptRunnerThread.class: $(patsubst %,$(BPATH)/%.class,Config Utils Item)
$(BPATH)/ItemQueue.class: $(patsubst %,$(BPATH)/%.class,Item ScriptRunnerThread ItemNotFoundException)
$(BPATH)/DisplayThread.class: $(patsubst %,$(BPATH)/%.class,ItemQueue)
$(BPATH)/Motqueser.class: $(patsubst %,$(BPATH)/%.class,ServerThread Utils)
