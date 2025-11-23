all : pecheAuxMoules pecheAuxMoulesBoucle

pecheAuxMoules :
	javac PecheAuxMoules.java

pecheAuxMoulesBoucle :
	javac PecheAuxMoulesBoucle.java

clean :
	rm -rf *~ *.class

jar :
	if ! [ -e MG2D ]; then echo " !!!! MG2D introuvable !!!!"; exit 1; fi
	javac PecheAuxMoules.java
	mkdir META-INF
	touch META-INF/MANIFEST.MF
	echo "Manifest-Version: 1.0" >> META-INF/MANIFEST.MF
	echo "Main-Class: PecheAuxMoules" >> META-INF/MANIFEST.MF
	jar --create --file PecheAuxMoules.jar --manifest META-INF/MANIFEST.MF *.class images/*.png MG2D/*.class MG2D/geometrie/*.class
	rm -rf *~ *.class MG2D META-INF
