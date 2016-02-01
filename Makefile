JAVAC = javac
JAVA = java
PRGM = Server
src = $(wildcard *.java)


run:


.PHONY: all
all: $(src)
	$(JAVAC) $?


.PHONY: run
run: all
	$(JAVA) $(PRGM)
