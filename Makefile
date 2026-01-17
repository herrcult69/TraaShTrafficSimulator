javac_dependence = -cp "lib/TraaS.jar:lib/pdfbox-2.0.30.jar:lib/fontbox-2.0.30.jar:lib/commons-logging-1.2.jar:lib/javafx/*" -d bin
java_dependence = --module-path "lib/javafx" --add-modules javafx.controls,javafx.graphics,javafx.swing -cp "lib/TraaS.jar:lib/pdfbox-2.0.30.jar:lib/fontbox-2.0.30.jar:lib/commons-logging-1.2.jar:bin"
source = src/*.java

compile:
	javac $(javac_dependence) $(source)
run: compile
	java $(java_dependence) TrafficSimulatorApp
clean:
	rm -f bin/*.class
