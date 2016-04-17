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
SRC_DIR := $(ROOT_DIR)/src
LIB_DIR := $(ROOT_DIR)/libs
TEST_DIR := $(ROOT_DIR)/test
BUILD_DIR := $(ROOT_DIR)/build
DIST_DIR := $(BUILD_DIR)/dist
JAR_DIR := $(BUILD_DIR)/jar
BPATH := $(JAR_DIR)/$(PKG)
empty :=
space := $(empty) $(empty)

src := $(wildcard $(SRC_DIR)/*.java)
objects := $(patsubst $(SRC_DIR)/%.java,$(BPATH)/%.class,$(src))
libs = $(wildcard $(LIB_DIR)/*.jar)
res := $(BPATH)/version.properties

test_src := $(wildcard $(TEST_DIR)/*.java)
test_objects := $(patsubst $(TEST_DIR)/%.java,$(BUILD_DIR)/%.class,$(test_src))
test_libs := $(wildcard $(TEST_DIR)/libs/*.jar)


all:


.PHONY: git-commit-check
git-commit-check:
	@(exit $$(git status --porcelain -uno | wc -l)) || (echo -e "********\n**** You have uncommitted changes\n********"; false)


.PHONY: dist
dist: jar | git-commit-check
	@[ -d $(DIST_DIR)/$(PRGM) ] || mkdir -p $(DIST_DIR)/$(PRGM)
	cp $(ROOT_DIR)/$(PRGM).jar $(LIB_DIR)/*.jar $(DIST_DIR)/$(PRGM)/
	cp $(PRGM).sh $(DIST_DIR)/$(PRGM)/
	cp $(PRGM).conf.sample $(DIST_DIR)/$(PRGM)/
	cd $(DIST_DIR)/ && tar -cf $(PRGM)-$(VERSION).tar $(PRGM)/
	cd $(DIST_DIR)/ && bzip2 -f $(PRGM)-$(VERSION).tar


.PHONY: jar
jar: $(BUILD_DIR)/$(PRGM)-$(VERSION).jar
	ln -sf $< $(ROOT_DIR)/$(PRGM).jar


$(BUILD_DIR)/$(PRGM)-$(VERSION).jar: $(objects) $(res) $(JAR_DIR)
	jar -cf $@ -C $(JAR_DIR) .


.PHONY: all
all: $(objects) $(res)


.PHONY: run
run: $(objects) $(res)
	$(JAVA) $(JAVA_ARGS) -cp $(LIB_DIR)/*:$(JAR_DIR) $(subst /,.,$(PKG)/$(PRGM)) $(ARGS)


.PHONY: test
test: $(test_objects) $(res)
	$(JAVA) -ea $(JAVA_ARGS) -cp $(LIB_DIR)/*:$(TEST_DIR)/libs/*:$(BUILD_DIR):$(JAR_DIR) Test


first_test_obj := $(firstword $(test_objects))
rest_test_obj := $(wordlist 2,$(words $(test_objects)),$(test_objects))

$(first_test_obj): $(test_src) $(libs) $(BUILD_DIR)
	$(JAVAC) $(JAVAC_ARGS) -cp $(LIB_DIR)/*:$(BUILD_DIR) -d $(BUILD_DIR) $(test_src)

$(rest_test_obj): $(first_test_obj)



$(BPATH): $(JAR_DIR)
$(JAR_DIR): $(BUILD_DIR)
$(BUILD_DIR) $(JAR_DIR) $(BPATH):
	@[ -d $@ ] || mkdir -p $@


.PHONY: distclean
distclean: clean
	@rm -v $(PRGM)-*.tar.bz2 2>/dev/null || true

.PHONY: clean
clean:
	@[ ! -e $(BUILD_DIR) ] || rm -rv $(BUILD_DIR)
	@[ ! -e $(DIST_DIR) ] || rm -rv $(DIST_DIR)
	@rm -v $(PRGM).jar 2>/dev/null || true


$(LIB_DIR):
	@[ -d $(LIB_DIR) ] || mkdir $(LIB_DIR)


.PHONY: libjars
libjars: $(LIB_DIR) $(libs)


$(libs) $(test_libs):
	$(MAKE) -C $(dir $(shell readlink $@)) jar


$(BPATH)/version.properties: $(BPATH)
	@echo vcs_version=$(VERSION) > $(BPATH)/version.properties


first_obj := $(firstword $(objects))
rest_obj := $(wordlist 2,$(words $(objects)),$(objects))

$(first_obj): $(src) $(libs) $(JAR_DIR)
	$(JAVAC) $(JAVAC_ARGS) -cp $(LIB_DIR)/*:$(JAR_DIR) -d $(JAR_DIR) $(src)

$(rest_obj): $(first_obj)
