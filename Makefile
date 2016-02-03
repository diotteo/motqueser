JAVA := java
JAVAC := javac
JAVAC_ARGS := -Xlint:unchecked

PRGM := Server
src := $(wildcard *.java)
objects := $(patsubst %.java,%.class,$(src))

libs = libs/java-getopt.jar


run:


.PHONY: all
all: $(objects) $(libs)


$(objects): %.class: %.java
	$(JAVAC) $(JAVAC_ARGS) -cp $(subst " ",":",$(libs)):. $^


.PHONY: run
run: all
	$(JAVA) -cp $(subst " ",":",$(libs)):. $(PRGM) $(ARGS)


.PHONY: libs
libs: $(libs)


libs/java-getopt.jar:
	@cd ../java-getopt/ && $(MAKE) java-getopt.jar
	@mkdir libs || true
	@cp ../java-getopt/java-getopt.jar libs/java-getopt.jar
