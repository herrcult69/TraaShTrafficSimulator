javac_dependence = -cp "lib/TraaS.jar:lib/javafx/*" -d bin
java_dependence = --module-path "lib/javafx" --add-modules javafx.controls,javafx.graphics -cp "lib/TraaS.jar:bin"
source = src/*.java

compile:
	javac $(javac_dependence) $(source)
run: compile
	java $(java_dependence) TrafficSimulatorApp
clean:
	rm -f bin/*.class

