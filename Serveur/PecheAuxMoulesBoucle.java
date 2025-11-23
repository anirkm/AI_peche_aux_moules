import java.net.*;
import java.io.*;
import MG2D.*;
import MG2D.geometrie.*;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.awt.Font;
import javax.swing.JFrame;
import java.awt.GraphicsEnvironment;
import java.awt.GraphicsDevice;

class PecheAuxMoulesBoucle{

    public static int tailleCase=32;
    public static int tailleX=25;
    public static int tailleY=17;
    public static boolean screenshot=false;
    public static int tour=0;
    public static int ecran=0;
    public static Labyrinthe la=null;
    public static Fenetre f = null;
    public static ArrayList<Texture> textureJoueur = null;
    public static ArrayList<Texture> textureTresor = null;
    public static ArrayList<Texture> texturePoint = null;
    public static ArrayList<Texture> texture3Pas = null;
    public static ArrayList<Texture> textureSaut = null;
    public static String cheminEnregistrerImage = "./";
    public static ArrayList<Texte> nomsJoueur = null;
    public static ArrayList<Texte> scores = null;
    public static String msgRecu="";
    public static char dernierCoup[];

	public static ServerSocket s;
    

    public static void usage(){
	System.out.println("java ServeurEpreuve1 <option> <valeur> <option> <valeur> ...");
	System.out.println("     avec option =");
	System.out.println("-tailleX <int> : modifie la taille en X du plateau");
	System.out.println("-tailleY <int> : modifie la taille en Y du plateau");
	System.out.println("-numLaby <int> : spécifie le numéro du labyrinthe pour la taille définie");
	System.out.println("-numPlacementBonus <int> : spécifie le numéro de placement des bonus pour le layrinthe");
	System.out.println("-nbJoueur <int> : modifie le nombre de joueur (doit être compris entre 1 et 4");
	System.out.println("-tauxDeMur <int> : modifie le taux de murs (doit être compris entre 0 et 50)");
	System.out.println("-nbSaut <int> : modifie le nombre de bonus saut dans le niveau");
	System.out.println("-nb3Pas <int> : modifie le nombre de bonus 3 pas dans le niveau");
	System.out.println("-nbTresor <int> : modifie le nombre de trésor dans le niveau");
	System.out.println("-tailleCase <int> : modifie la taille des cases pour l'affichage");
	System.out.println("-port <int> : modifie le port pour le serveur");
	System.out.println("-delay <int> : modifie le temps d'attente minimum entre deux tours");
	System.out.println("-timeout <int> : modifie le temps d'attente maximum entre deux tours");
	System.out.println("-nbTourMax <int> : fixer un nombre de tours maximal ****par joueur****");
	System.out.println("-screenshot <String> : permet de prendre des screenshots à chaque mouvement de d'enregistrer les images dans le répertoire passé en paramètre");
	System.out.println("-ecran <int> : permet de donner le numéro de l'écran sur lequel afficher l'application (si plusieurs écrans sont branchés au pc)");
	System.exit(0);
    }

    public static void showOnScreen( int screen, JFrame frame ) {
	GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
	GraphicsDevice[] gd = ge.getScreenDevices();
	if( screen > -1 && screen < gd.length ) {
	    frame.setLocation(gd[screen].getDefaultConfiguration().getBounds().x, frame.getY());
	} else if( gd.length > 0 ) {
	    frame.setLocation(gd[0].getDefaultConfiguration().getBounds().x, frame.getY());
	} else {
	    throw new RuntimeException( "L'écran pour l'affichage n'a pas été trouvé." );
	}
    }


    public static String getDate(){
	String jour=new SimpleDateFormat("dd", Locale.FRANCE).format(new Date());
	String mois=new SimpleDateFormat("MM", Locale.FRANCE).format(new Date());
	String annee=new SimpleDateFormat("yyyy", Locale.FRANCE).format(new Date());
	String heure=new SimpleDateFormat("HH", Locale.FRANCE).format(new Date());
	String minute=new SimpleDateFormat("mm", Locale.FRANCE).format(new Date());
	String seconde=new SimpleDateFormat("ss", Locale.FRANCE).format(new Date());
	return jour+mois+annee+"-"+heure+minute+seconde;
    }

    public static String getSimpleDate(){
	String jour=new SimpleDateFormat("dd", Locale.FRANCE).format(new Date());
	String mois=new SimpleDateFormat("MM", Locale.FRANCE).format(new Date());
	String annee=new SimpleDateFormat("yyyy", Locale.FRANCE).format(new Date());
	String heure=new SimpleDateFormat("HH", Locale.FRANCE).format(new Date());
	String minute=new SimpleDateFormat("mm", Locale.FRANCE).format(new Date());
	return jour+mois+annee+"-"+heure+minute;
    }

    private static String getNomFichier(String date, int n){
	if(n<10)
	    return date+"_0000"+n;
	if(n<100)
	    return date+"_000"+n;
	if(n<1000)
	    return date+"_00"+n;
	if(n<10000)
	    return date+"_0"+n;
	return date+"_"+n;
    }

    private static String getNomFichierImageBloc(int x, int y){
	//si le bloc n'est pas un mur, on retourne le bloc sable
	if(la.getXY(x,y).getType()!=Case.MUR)
	    return "bloc13.png";
	if(x==0){
	    if(y==0)
		//coin haut gauche
		return "bloc01.png";
	    if(y==tailleY)
		//coin bas gauche
		return "bloc03.png";
	    //partie gauche
	    if(la.getXY(x+1,y).getType()==Case.MUR)
		//partie gauche mais avec un embranchement à droite
		return "bloc10.png";
	    //partie gauche classique
	    return "bloc06.png";
	}
	if(x==tailleX-1){
	    if(y==0)
		//coin haut droite
		return "bloc02.png";
	    if(y==tailleY-1)
		//coin bas droite
		return "bloc04.png";
	    //partie droite
	    if(la.getXY(x-1,y).getType()==Case.MUR)
		//partie droite mais avec un embranchement à gauche
		return "bloc11.png";
	    //partie droite classique
	    return "bloc07.png";
	}
	if(y==0){
	    //partie haute
	    if(la.getXY(x,y+1).getType()==Case.MUR)
		//partie haute mais avec un embranchement en bas
		return "bloc09.png";
	    //partie haute classique
	    return "bloc05.png";
	}
	if(y==tailleY-1){
	    //partie basse
	    if(la.getXY(x,y-1).getType()==Case.MUR)
		//partie basse mais avec un embranchement en haut
		return "bloc12.png";
	    //partie basse classique
	    return "bloc08.png";
	}

	// cas d'une dun mur isolée sans voisin
	// ooo
	// oxo
	// ooo
	if(la.getXY(x,y-1).getType()!=Case.MUR && la.getXY(x-1,y).getType()!=Case.MUR && la.getXY(x+1,y).getType()!=Case.MUR && la.getXY(x,y+1).getType()!=Case.MUR)
	    return "bloc14.png";
	// ooo
	// oxo
	// oxo
	if(la.getXY(x,y-1).getType()!=Case.MUR && la.getXY(x-1,y).getType()!=Case.MUR && la.getXY(x+1,y).getType()!=Case.MUR && la.getXY(x,y+1).getType()==Case.MUR)
	    return "bloc15.png";
	// ooo
	// oxx
	// ooo
	if(la.getXY(x,y-1).getType()!=Case.MUR && la.getXY(x-1,y).getType()!=Case.MUR && la.getXY(x+1,y).getType()==Case.MUR && la.getXY(x,y+1).getType()!=Case.MUR)
	    return "bloc16.png";
	// ooo
	// oxx
	// oxo
	if(la.getXY(x,y-1).getType()!=Case.MUR && la.getXY(x-1,y).getType()!=Case.MUR && la.getXY(x+1,y).getType()==Case.MUR && la.getXY(x,y+1).getType()==Case.MUR)
	    return "bloc17.png";
	// ooo
	// xxo
	// ooo
	if(la.getXY(x,y-1).getType()!=Case.MUR && la.getXY(x-1,y).getType()==Case.MUR && la.getXY(x+1,y).getType()!=Case.MUR && la.getXY(x,y+1).getType()!=Case.MUR)
	    return "bloc18.png";
	// ooo
	// xxo
	// oxo
	if(la.getXY(x,y-1).getType()!=Case.MUR && la.getXY(x-1,y).getType()==Case.MUR && la.getXY(x+1,y).getType()!=Case.MUR && la.getXY(x,y+1).getType()==Case.MUR)
	    return "bloc19.png";
	// ooo
	// xxx
	// ooo
	if(la.getXY(x,y-1).getType()!=Case.MUR && la.getXY(x-1,y).getType()==Case.MUR && la.getXY(x+1,y).getType()==Case.MUR && la.getXY(x,y+1).getType()!=Case.MUR)
	    return "bloc20.png";
	// ooo
	// xxx
	// oxo
	if(la.getXY(x,y-1).getType()!=Case.MUR && la.getXY(x-1,y).getType()==Case.MUR && la.getXY(x+1,y).getType()==Case.MUR && la.getXY(x,y+1).getType()==Case.MUR)
	    return "bloc21.png";
	// oxo
	// oxo
	// ooo
	if(la.getXY(x,y-1).getType()==Case.MUR && la.getXY(x-1,y).getType()!=Case.MUR && la.getXY(x+1,y).getType()!=Case.MUR && la.getXY(x,y+1).getType()!=Case.MUR)
	    return "bloc22.png";
	// oxo
	// oxo
	// oxo
	if(la.getXY(x,y-1).getType()==Case.MUR && la.getXY(x-1,y).getType()!=Case.MUR && la.getXY(x+1,y).getType()!=Case.MUR && la.getXY(x,y+1).getType()==Case.MUR)
	    return "bloc23.png";
	// oxo
	// oxx
	// ooo
	if(la.getXY(x,y-1).getType()==Case.MUR && la.getXY(x-1,y).getType()!=Case.MUR && la.getXY(x+1,y).getType()==Case.MUR && la.getXY(x,y+1).getType()!=Case.MUR)
	    return "bloc24.png";
	// oxo
	// oxx
	// oxo
	if(la.getXY(x,y-1).getType()==Case.MUR && la.getXY(x-1,y).getType()!=Case.MUR && la.getXY(x+1,y).getType()==Case.MUR && la.getXY(x,y+1).getType()==Case.MUR)
	    return "bloc25.png";
	// oxo
	// xxo
	// ooo
	if(la.getXY(x,y-1).getType()==Case.MUR && la.getXY(x-1,y).getType()==Case.MUR && la.getXY(x+1,y).getType()!=Case.MUR && la.getXY(x,y+1).getType()!=Case.MUR)
	    return "bloc26.png";
	// oxo
	// xxo
	// oxo
	if(la.getXY(x,y-1).getType()==Case.MUR && la.getXY(x-1,y).getType()==Case.MUR && la.getXY(x+1,y).getType()!=Case.MUR && la.getXY(x,y+1).getType()==Case.MUR)
	    return "bloc27.png";
	// oxo
	// xxx
	// ooo
	if(la.getXY(x,y-1).getType()==Case.MUR && la.getXY(x-1,y).getType()==Case.MUR && la.getXY(x+1,y).getType()==Case.MUR && la.getXY(x,y+1).getType()!=Case.MUR)
	    return "bloc28.png";
	// oxo
	// xxx
	// oxo
	//if(la.getXY(x,y-1).getType()==Case.MUR && la.getXY(x-1,y).getType()==Case.MUR && la.getXY(x+1,y).getType()==Case.MUR && la.getXY(x,y+1).getType()==Case.MUR)
	    return "bloc29.png";
    }

    public static void affichageLabyrinthe(){
	f.effacer();

	for(int i=0;i<tailleX;i++)
	    for(int j=0;j<tailleY;j++)
		f.ajouter(new Texture("images/"+getNomFichierImageBloc(i,j),new Point(i*tailleCase,(tailleY-j-1)*tailleCase),tailleCase,tailleCase));
	
	int tailleXBordMer = 6*tailleCase;
	int tailleYBordMer = (int)((6*tailleCase*1.0)*512.0/384.0);
	int nbRepetition=((tailleY*tailleCase)/tailleYBordMer)+1;

	for(int i=0;i<nbRepetition;i++)
	    f.ajouter(new Texture("images/bordMer2.png",new Point(tailleX*tailleCase,(i*tailleYBordMer)),tailleXBordMer,tailleYBordMer));

	for(int i=0;i<textureJoueur.size();i++)
	    f.ajouter(textureJoueur.get(i));
	for(int i=0;i<textureTresor.size();i++)
	    f.ajouter(textureTresor.get(i));
	for(int i=0;i<texturePoint.size();i++)
	    f.ajouter(texturePoint.get(i));
	for(int i=0;i<textureSaut.size();i++)
	    f.ajouter(textureSaut.get(i));
	for(int i=0;i<texture3Pas.size();i++)
	    f.ajouter(texture3Pas.get(i));

	for(int i=0;i<scores.size();i++){
	    f.ajouter(scores.get(i));
	    f.ajouter(nomsJoueur.get(i));
	}
    }

    public static void miseAJourLabyrinthe(){

	// Mise à jour de la position des joueurs
	
	for(int i=0;i<la.getNbJoueur();i++){
	    /*System.out.println("taille : "+tailleX+","+tailleY+","+tailleCase);
	    System.out.println("pos joueur : "+la.getJoueur(i).getPosX()+","+la.getJoueur(i).getPosY());
	    System.out.println("pos texture : "+textureJoueur.get(i).getA().getX()+","+textureJoueur.get(i).getA().getY());
	    System.out.println("pas de deplacement : "+(la.getJoueur(i).getPosX()*tailleCase-textureJoueur.get(i).getA().getX()));

	    System.out.println("pas de deplacement : "+(la.getJoueur(i).getPosY()*tailleCase-textureJoueur.get(i).getA().getX()));*/

	    /*PATCH de derniere minute.... C'est moche*/
	    f.supprimer(textureJoueur.get(i));
	    if(dernierCoup[i]!='C')
		textureJoueur.set(i,new Texture("images/joueur"+(i+1)+"_small_"+dernierCoup[i]+".png",new Point(la.getJoueur(i).getPosX()*tailleCase,(tailleY-la.getJoueur(i).getPosY()-1)*tailleCase),tailleCase,tailleCase));
	    else
		textureJoueur.set(i,new Texture("images/joueur"+(i+1)+"_small_N.png",new Point(la.getJoueur(i).getPosX()*tailleCase,(tailleY-la.getJoueur(i).getPosY()-1)*tailleCase),tailleCase,tailleCase));
	    f.ajouter(textureJoueur.get(i));
	    
	    //textureJoueur.get(i).setA(new Point(la.getJoueur(i).getPosX()*tailleCase,(tailleY-la.getJoueur(i).getPosY()-1)*tailleCase));
	    //textureJoueur.get(i).translater(la.getJoueur(i).getPosX()*tailleCase-textureJoueur.get(i).getA().getX(),);
	    if(dernierCoup[i]!='C'){
		textureJoueur.get(i).setImg("images/joueur"+(i+1)+"_small_"+dernierCoup[i]+".png");
		textureJoueur.get(i).setLargeur(tailleCase);
		textureJoueur.get(i).setHauteur(tailleCase);
	    }
	    for(int j=textureTresor.size()-1;j>=0;j--){
		// TODO - ca c'est moche, mais ca marche !
		// Tenter une nouvelle version de Labyrinthe avec des arraylist de bonus
		if(textureJoueur.get(i).getA().equals(textureTresor.get(j).getA())){
		    f.supprimer(textureTresor.get(j));
		    textureTresor.remove(j);
		    f.supprimer(texturePoint.get(j));
		    texturePoint.remove(j);
		}
	    }
	    for(int j=textureSaut.size()-1;j>=0;j--){
		if(textureJoueur.get(i).getA().equals(textureSaut.get(j).getA())){
		    f.supprimer(textureSaut.get(j));
		    textureSaut.remove(j);
		}
	    }
	    for(int j=texture3Pas.size()-1;j>=0;j--){
		if(textureJoueur.get(i).getA().equals(texture3Pas.get(j).getA())){
		    f.supprimer(texture3Pas.get(j));
		    texture3Pas.remove(j);
		}
	    }
	}

	for(int i=0;i<scores.size();i++){
	    scores.get(i).setTexte(""+la.getJoueur(i).getNbPoints());
	    nomsJoueur.get(i).setA(new Point((int)((tailleX+0.2)*tailleCase+nomsJoueur.get(i).getLargeur()/2),nomsJoueur.get(i).getA().getY()));
	}
	
	// Rafraichissement de la fenetre
	f.rafraichir();
	
    }
    
    public static void main(String args[]){

	while (true){

	int nbJoueurs=1;
	int tauxMur=50;
	int nbSaut=8;
	int nbTresor=10;
	int nb3Pas=5;
	int port=1337;
	long nbLaby=-1;
	long numPlacementBonus=-1;
	long delay=100;
	int timeout=3000;
	int nbTourMax=-1;

	Scanner sc = new Scanner(System.in);

	long tpsDepart=new Date().getTime();
	long tpsCourant;

	ArrayList<Character> derniersCoups = new ArrayList<Character>();

	// TRAITEMENT DES OPTIONS
	// OPTIONS POSSIBLES :
	// -tailleX <int> : modifie la taille en X du plateau
	// -tailleY <int> : modifie la taille en Y du plateau
	// -nbJoueur <int> : modifie le nombre de joueur (doit être compris entre 1 et 4
	// -tauxDeMur <int> : modifie le taux de murs (doit être compris entre 0 et 50)
	// -nbSaut <int> : modifie le nombre de bonus saut dans le niveau
	// -nb3Pas <int> : modifie le nombre de bonus 3 pas dans le niveau
	// -nbTresor <int> : modifie le nombre de trésor dans le niveau
	// -tailleCase <int> : modifie la taille des cases pour l'affichage
	// -port <int> : modifie le port pour le serveur
	// -delay <int> : modifie le temps d'attente minimum entre deux tours
	// -timeout <int> : modifie le temps d'attente maximum entre deux tours
	// -nbTourMax <int> : fixe un nombre de tours maximal ****par joueur****
	// -screenshot : permet de prendre des screenshots à chaque mouvement
	// -ecran : permet d'afficher l'application sur un second écran

	for(int i=0;i<args.length;i++){
	    switch(args[i]){
	    case "-tailleX" :
		i++;
		tailleX=Integer.parseInt(args[i]);
		break;
	    case "-tailleY" :
		i++;
		tailleY=Integer.parseInt(args[i]);
		break;
	    case "-nbJoueur" :
		i++;
		nbJoueurs=Integer.parseInt(args[i]);
		if(nbJoueurs<1 || nbJoueurs>4){
		    System.out.println("Le nombre de joueur doit être compris entre 1 et 4");
		    System.exit(0);
		}
		break;
	    case "-tauxDeMur" :
		i++;
		tauxMur=Integer.parseInt(args[i]);
		if(tauxMur<0 || tauxMur>50){
		    System.out.println("Le taux de mur doit être compris entre 0 et 50");
		    System.exit(0);
		}
		break;
	    case "-nbSaut" :
		i++;
		nbSaut=Integer.parseInt(args[i]);
		break;
	    case "-nb3Pas" :
		i++;
		nb3Pas=Integer.parseInt(args[i]);
		break;
	    case "-nbTresor" :
		i++;
		nbTresor=Integer.parseInt(args[i]);
		break;
	    case "-tailleCase" :
		i++;
		tailleCase=Integer.parseInt(args[i]);
		break;
	    case "-port" :
		i++;
		port=Integer.parseInt(args[i]);
		break;
	    case "-numLaby" :
		i++;
		nbLaby=Long.parseLong(args[i]);
		break;
	    case "-numPlacementBonus" :
		i++;
		numPlacementBonus=Long.parseLong(args[i]);
		break;
	    case "-delay" :
		i++;
		delay=Long.parseLong(args[i]);
		break;
	    case "-timeout" :
		i++;
		timeout=Integer.parseInt(args[i]);
		break;
	    case "-nbTourMax" :
		i++;
		nbTourMax=Integer.parseInt(args[i]);
		break;
	    case "-screenshot" :
		screenshot=true;
		i++;
		cheminEnregistrerImage=args[i];
		break;
	    case "-ecran" :
		i++;
		ecran=Integer.parseInt(args[i]);
		break;
	    default :
		System.out.println("Je ne comprends pas l'option "+args[i]);
		usage();
	    }
	}

	//Remplissage des derniers coups par de vrais coups
	for(int i=0;i<nbJoueurs;i++)
	    derniersCoups.add('E');

	//création du labyrinthe
	if(nbLaby==-1)
	    nbLaby=(long)(Math.random()*65535);
	if(numPlacementBonus==-1)
	    numPlacementBonus=(long)(Math.random()*65535);

	la = new Labyrinthe(tailleX,tailleY,nbLaby,numPlacementBonus);
	
	la.simplifierLabyrinthe(tauxMur);
	la.ajouterJoueur(1,1);
	if(nbJoueurs>1)
	    la.ajouterJoueur(tailleX-2,tailleY-2);
	if(nbJoueurs>2)
	    la.ajouterJoueur(tailleX-2,1);
	if(nbJoueurs>3)
	    la.ajouterJoueur(1,tailleY-2);

	for(int i=0;i<nbTresor;i++)
	    la.placerBonus(Case.TRESOR);

	for(int i=0;i<nb3Pas;i++)
	    la.placerBonus(Case.BONUS3PAS);

	for(int i=0;i<nbSaut;i++)
	    la.placerBonus(Case.BONUSSAUT);

	textureJoueur = new ArrayList<Texture>();
	//for(int i=0;i<la.getNbJoueur();i++)
	//    textureJoueur.add(new Texture("images/joueur"+(i+1)+"_small_E.png",new Point(la.getJoueur(i).getPosX()*tailleCase,(tailleY-la.getJoueur(i).getPosY()-1)*tailleCase),tailleCase,tailleCase));
	textureJoueur.add(new Texture("images/joueur1_small_E.png",new Point(la.getJoueur(0).getPosX()*tailleCase,(tailleY-la.getJoueur(0).getPosY()-1)*tailleCase),tailleCase,tailleCase));
	if(nbJoueurs>1)
	    textureJoueur.add(new Texture("images/joueur2_small_O.png",new Point(la.getJoueur(1).getPosX()*tailleCase,(tailleY-la.getJoueur(1).getPosY()-1)*tailleCase),tailleCase,tailleCase));
	if(nbJoueurs>2)
	    textureJoueur.add(new Texture("images/joueur3_small_S.png",new Point(la.getJoueur(2).getPosX()*tailleCase,(tailleY-la.getJoueur(2).getPosY()-1)*tailleCase),tailleCase,tailleCase));
	if(nbJoueurs>3)
	    textureJoueur.add(new Texture("images/joueur4_small_N.png",new Point(la.getJoueur(3).getPosX()*tailleCase,(tailleY-la.getJoueur(3).getPosY()-1)*tailleCase),tailleCase,tailleCase));

	dernierCoup = new char[4];
	dernierCoup[0]='E';
	dernierCoup[1]='O';
	dernierCoup[2]='S';
	dernierCoup[3]='N';


	textureTresor = new ArrayList<Texture>();
	texturePoint = new ArrayList<Texture>();
	texture3Pas = new ArrayList<Texture>();
	textureSaut = new ArrayList<Texture>();
	scores = new ArrayList<Texte>();
	nomsJoueur = new ArrayList<Texte>();
	
	for(int i=0;i<tailleX;i++)
	    for(int j=0;j<tailleY;j++){
		if(la.getXY(i,j).getType()==Case.TRESOR){
		    textureTresor.add(new Texture("images/tresor_small.png",new Point(i*tailleCase,(tailleY-j-1)*tailleCase),tailleCase,tailleCase));
		    texturePoint.add(new Texture("images/"+la.getXY(i,j).getPointRapporte()+".png",new Point(i*tailleCase,(tailleY-j-1)*tailleCase),tailleCase,tailleCase));		    
		}
		if(la.getXY(i,j).getType()==Case.BONUSSAUT)
		    textureSaut.add(new Texture("images/bonusSaut_small.png",new Point(i*tailleCase,(tailleY-j-1)*tailleCase),tailleCase,tailleCase));
		if(la.getXY(i,j).getType()==Case.BONUS3PAS)
		    texture3Pas.add(new Texture("images/bonus3pas_small.png",new Point(i*tailleCase,(tailleY-j-1)*tailleCase),tailleCase,tailleCase));
	    }

	int hauteur=tailleY*tailleCase;
	int multiplicateur=hauteur/5;
	int tailleFont=hauteur/14;
	if(tailleFont>25)
	    tailleFont=25;
	int addition=tailleFont;

	
	// Création de la fenêtre d'affichage
	f = new Fenetre("La chasse au trésor",(tailleX+6)*tailleCase,tailleY*tailleCase);
	showOnScreen(ecran,f);

	// Affichage d'un signal d'attente

	Rectangle chargementExterieur = new Rectangle(Couleur.NOIR,new Point(tailleCase,((tailleY*tailleCase)-tailleCase)/2),new Point((tailleX*tailleCase)-tailleCase,((tailleY*tailleCase)+tailleCase)/2),false);
	Rectangle chargementInterieur = new Rectangle(Couleur.ROUGE,new Point(tailleCase,((tailleY*tailleCase)-tailleCase)/2),0,tailleCase,true);
	f.ajouter(chargementInterieur);
	f.ajouter(chargementExterieur);
	f.rafraichir();
	
	// Création de la socket
	int nbConnecte=0;
	String connecte[] = new String[nbJoueurs];
	Socket services[] = new Socket[nbJoueurs];
	String nomFichier="logServeurLaby-"+getDate();
	FileWriter fw=null;
	BufferedWriter output=null;
	String dateLancement=getSimpleDate();
	
	try{
	    fw = new FileWriter(nomFichier, true);
	    output = new BufferedWriter(fw);
	}
	catch(IOException e){
	    System.out.println("Impossible de créer le fichier "+nomFichier);
	    System.exit(0);
	}

	// Ajout de toutes les infos de la configuration de la partie dans les logs
	try{
	    output.write("Création d'un labyrinthe de "+tailleX+"x"+tailleY+" dont les cases font "+tailleCase+" pixels.\n");
	    output.flush();
	    output.write(la+"\n");
	    output.flush();
	    if(nbLaby==-1)
		output.write("Le labyrinthe a été généré aléatoirement.\n");
	    else
		output.write("Le labyrinthe porte le numéro "+nbLaby+".\n");
	    output.flush();
	    if(numPlacementBonus==-1)
		output.write("Le placement des bonus a été généré aléatoirement\n");
	    else
		output.write("Le placement des bonus porte le numéro "+numPlacementBonus+";\n");
	    output.flush();
	    output.write("Il y a "+nbJoueurs+" joueur(s)\n");
	    output.flush();
	    output.write("Le labyrinthe contient un taux de mur de "+tauxMur+"%, "+nbSaut+" bonus saut, "+nbTresor+" trésor(s) et "+nb3Pas+" bonus 3 pas\n");
	    output.flush();
	    output.write("Le port du serveur est le "+port+". Le timeout pour chaque joueur est de "+timeout+" ms. le délai d'attente minimum entre deux tours est de "+delay+" ms.\n");
	    output.flush();
	    output.write("Les screenshots à chaque tour ne sont pas activés\n");
	    output.flush();
	}catch(IOException e){
	    System.out.println("Problème lors de l'écriture dans le fichier "+nomFichier);
	    System.exit(0);
	}
	System.out.println("Le fichier de log est : "+nomFichier);
	System.out.println("Le labyrinthe porte le numéro "+nbLaby);
	System.out.println("Le placement des bonus porte le numéro "+numPlacementBonus);
	System.out.println("Le labyrinthe de "+tailleX+"x"+tailleY+" contient un taux de mur de "+tauxMur+"%, "+nbSaut+" bonus saut, "+nbTresor+" trésor(s) et "+nb3Pas+" bonus 3 pas");

	if (s == null){
		try{
			s = new ServerSocket(port);

			System.out.println("Serveur en attente de connexion");
			output.write("Serveur en attente de connexion\n");
			output.flush();
		}
		catch(IOException e){
			System.out.println("Impossible de créer la socket sur le port "+port);
			System.exit(0);
		}
	}

	// Attente des joueurs - attendre le bon nombre de joueur
	while(nbConnecte!=nbJoueurs){
	    try{
		services[nbConnecte] = s.accept();
		services[nbConnecte].setSoTimeout(timeout);
	    }
	    catch(IOException e){
		System.out.println("Problème lors de la connexion d'une équipe "+port);
		System.exit(0);
	    }
	    InetSocketAddress adresse = (InetSocketAddress)services[nbConnecte].getRemoteSocketAddress();
	    
	    String ip= adresse.toString();
	    connecte[nbConnecte]=ip.split(":")[0];
	    
	    String hostname = adresse.getHostName();
	    
	    System.out.println("************************************************************\n*    -->    "+new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss", Locale.FRANCE).format(new Date())+"                          *\n*       Connexion "+nbConnecte+" - adresse : "+hostname+", "+ip+"*\n************************************************************");
	    try{
		output.write("************************************************************\n*    -->    "+new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss", Locale.FRANCE).format(new Date())+"                          *\n*       Connexion "+nbConnecte+" - adresse : "+hostname+", "+ip+"\n************************************************************\n");
		output.flush();
	    }catch(IOException e){
		System.out.println("Problème lors de l'écriture dans le fichier "+nomFichier);
		System.exit(0);
	    }
	    
	    nbConnecte++;
	    chargementInterieur.setLargeur((int)((tailleX*tailleCase-2*tailleCase)*((nbConnecte*1.0)/(nbJoueurs*1.0))));
	    f.rafraichir();
	}
	
	// Une fois tout le monde connecté, on envoie les infos du laby à tout le monde
	InputStream is[] = new InputStream[nbJoueurs];
	OutputStream os[] = new OutputStream[nbJoueurs];
	BufferedReader bf[] = new BufferedReader[nbJoueurs];
	PrintWriter pw[] = new PrintWriter[nbJoueurs];
	for(int i=0;i<nbJoueurs;i++){
	    is[i]=null;
	    try{
		is[i] = services[i].getInputStream();
	    }catch(IOException e){
		System.out.println("Impossible de récupérer un canal de communication en lecture pour le joueur "+i);
		System.exit(0);
	    }
	    try{
		os[i]  = services[i].getOutputStream();
	    }catch(IOException e){
		System.out.println("Impossible de récupérer un canal de communication en écriture pour le joueur "+i);
		System.exit(0);
	    }
	    bf[i] = new BufferedReader(new InputStreamReader(is[i]));
	    pw[i] = new PrintWriter(os[i]);
	}

	//On récupère les noms des participants
	for(int i=0;i<nbJoueurs;i++){
	    Couleur c=new Couleur(19,172,172);
	    if(i==1)
		c=new Couleur(160,23,23);
	    if(i==2)
		c=new Couleur(172,188,0);
	    if(i==3)
		c=new Couleur(56,56,56);
	    try{
		msgRecu=bf[i].readLine();
		nomsJoueur.add(new Texte(c,msgRecu,new Font("Calibri",Font.TYPE1_FONT,tailleFont),new Point((tailleX+2)*tailleCase,tailleY*tailleCase-(i+1)*multiplicateur+addition)));
		nomsJoueur.get(i).setA(new Point((int)((tailleX+0.2)*tailleCase),tailleY*tailleCase-(i+1)*multiplicateur+addition));
		scores.add(new Texte(c,"0",new Font("Calibri",Font.TYPE1_FONT,tailleFont),new Point((tailleX+2)*tailleCase,tailleY*tailleCase-(i+1)*multiplicateur)));
	    }catch(IOException e){
		System.out.println("Impossible de lire le nom du joueur "+i);
		nomsJoueur.add(new Texte(c,"Les blaireaux",new Font("Calibri",Font.TYPE1_FONT,tailleFont),new Point((tailleX+2)*tailleCase,tailleY*tailleCase-(i+1)*multiplicateur+addition)));
		nomsJoueur.get(i).setA(new Point((int)((tailleX+0.2)*tailleCase),tailleY*tailleCase-(i+1)*multiplicateur+addition));
		scores.add(new Texte(c,"0",new Font("Calibri",Font.TYPE1_FONT,tailleFont),new Point((tailleX+2)*tailleCase,tailleY*tailleCase-(i+1)*multiplicateur)));
	    }
	}

	// On envoie le numéro de joueur à chaque participant
	for(int i=0;i<nbJoueurs;i++){
	    pw[i].println(""+i);
	    pw[i].flush();
	    System.out.println(getDate()+" - envoi du numéro de joueur au joueur "+i);
	    try{
		output.write(getDate()+" -  envoi du numéro de joueur au joueur "+i+"\n");
		output.flush();
	    }catch(IOException e){
		System.out.println("Problème lors de l'écriture dans le fichier "+nomFichier);
		System.exit(0);
	    }
	}
	
	int numJoueur=-1;
	tour=-1;
	boolean toutLeMondeConnecte=true;

	affichageLabyrinthe();
	
	// Dès que tout le monde est connecté, on lance la boucle.
	// TODO - vérifier si le nombre de tour maximal est atteint
	while(nbTresor>0 && toutLeMondeConnecte && (nbTourMax==-1 || ((tour+1)<nbTourMax*nbJoueurs))){
	    tour++;
	    String infoLaby = la.toString();
	    numJoueur=(numJoueur+1)%nbJoueurs;

	    // on renvoie toutes les infos du laby à chaque tour

	    miseAJourLabyrinthe();

	    tpsCourant=new Date().getTime();
	    //System.out.println("temps d'attente : "+(delay-((tpsCourant-tpsDepart)/1000)));
	    try{Thread.sleep(delay-((tpsCourant-tpsDepart)/1000));}catch(java.lang.Exception e){}
	    tpsDepart=tpsCourant;
	    
	    pw[numJoueur].println(infoLaby);
	    pw[numJoueur].flush();
	    System.out.println(getDate()+" - envoi des infos du labyrinthe au joueur "+numJoueur);
	    try{
		output.write(getDate()+" -  envoi des infos du labyrinthe au joueur "+numJoueur+"\n");
		output.flush();
	    }catch(IOException e){
		System.out.println("Problème lors de l'écriture dans le fichier "+nomFichier);
		System.exit(0);
	    }

	    // On attend la réponse
	    msgRecu="";
	    try{
		msgRecu=bf[numJoueur].readLine();
	    }catch(IOException e){
		// Cas
		System.out.println("Impossible de lire les données sur la socket pour le joueur "+numJoueur);
		System.out.println("Le mouvement joué sera C pour le joueur "+numJoueur);
		msgRecu="C";
	    }
	    if(msgRecu==null){
		System.out.println("Le joueur "+numJoueur+" s'est surement deconnecté car le message est illisible");
		System.out.println("Le mouvement joué sera C pour le joueur "+numJoueur);
		msgRecu="C";
	    }
	    System.out.println(getDate()+" - réception du message \""+msgRecu+"\" de la part du joueur "+numJoueur);
	    try{
		output.write(getDate()+" - réception du message \""+msgRecu+"\" de la part du joueur "+numJoueur+"\n");
		output.flush();
	    }catch(IOException e){
		System.out.println("Problème lors de l'écriture dans le fichier "+nomFichier);
		System.exit(0);
	    }

	    derniersCoups.remove(0);
	    derniersCoups.add(msgRecu.charAt(0));

	    // Execution de la commande suivante
	    String msgRetourExecution=la.executerCommande("J"+numJoueur+"-"+msgRecu);
	    if(msgRecu!=null)
		dernierCoup[numJoueur]=msgRecu.charAt(msgRecu.length()-1);
	    // ajout du retour dans les logs
	    try{
		output.write("Retour de l'exécution de la commande : "+msgRetourExecution+"\n");
		output.flush();
	    }catch(IOException e){
		System.out.println("Problème lors de l'écriture dans le fichier "+nomFichier);
		System.exit(0);
	    }

	    nbTresor=0;
	    for(int i=0;i<tailleX;i++)
		for(int j=0;j<tailleY;j++)
		    if(la.getXY(i,j).getType()==Case.TRESOR)
			nbTresor++;

	    // On vérifie que tout le monde est encore connecté
	    // Inutile finalement car le cas est géré par les timeout.
	    /*for(int i=0;i<nbJoueurs;i++)
	      toutLeMondeConnecte=(toutLeMondeConnecte && services[i].isConnected());*/

	    // Vérifier que les derniers coups joués ne sont pas tous des "C"
	    // Dans ce cas, cela voudrait dire que tout le monde est déconnecté, que tout le monde a passé ou un mixte des deux.
	    // Dans ce cas, on arrête la simulation.

	    // Vérifie qu'au moins un joueur a joué
	    toutLeMondeConnecte=false;
	    for(int i=0;i<derniersCoups.size();i++)
		if(derniersCoups.get(i)!='C')
		    toutLeMondeConnecte=true;

	    if(toutLeMondeConnecte==false){
		try{
		    output.write("Tous les participants ont passé ou sont déconnectés\n");
		    output.flush();
		}catch(IOException e){
		    System.out.println("Problème lors de l'écriture dans le fichier "+nomFichier);
		    System.exit(0);
		}
	    }

	    if(screenshot)
		f.snapShot(cheminEnregistrerImage+"/"+getNomFichier(dateLancement,tour)+".jpg");
	}
	tour++;
	miseAJourLabyrinthe();
	tpsCourant=new Date().getTime();
	try{Thread.sleep(delay-((tpsCourant-tpsDepart)/1000));}catch(java.lang.Exception e){}
	
	if(screenshot)
	    f.snapShot(cheminEnregistrerImage+"/"+getNomFichier(dateLancement,tour)+".jpg");

	if(toutLeMondeConnecte ){
	    if(nbTourMax!=-1 && tour>=nbTourMax*nbJoueurs)
		System.out.println("Nombre de tour maximal par joueur dépassé.");
	    else
		System.out.println("Tous les trésors ont été ramassés en "+tour+" tours.");
	}
	else
	    System.out.println("Fin de partie au bout de "+tour+"+ tours. Quelques trésors se sont échappés.");
	
	try{
	    if(toutLeMondeConnecte){
		if(nbTourMax!=-1 && tour>=nbTourMax*nbJoueurs)
		    output.write("Nombre de tour maximal par joueur dépassé.\n");
		else
		    output.write("Tous les trésors ont été ramassées en "+tour+" tours.\n");
	    }
	    else
		output.write("Fin de partie en "+tour+" tours. Quelques trésors se sont échappés.\n");
	    output.flush();
	}catch(IOException e){
	    System.out.println("Problème lors de l'écriture dans le fichier "+nomFichier);
	    System.exit(0);
	}
	    
	for(int i=0;i<nbJoueurs;i++){
	    pw[i].println("FIN");
	    pw[i].flush();
	    System.out.println(getDate()+" - envoi de la fin de partie au joueur "+i);
	    try{
		output.write(getDate()+" -  envoi de la fin de partie au joueur "+i+"\n");
		output.flush();
	    }catch(IOException e){
		System.out.println("Problème lors de l'écriture dans le fichier "+nomFichier);
		System.exit(0);
	    }
	}

	try{
	    System.out.println("---------------------------");
	    output.write("---------------------------\n");
	    output.flush();
	    for(int i=0;i<la.getNbJoueur();i++){
		System.out.println(la.getJoueur(i));
		output.write(la.getJoueur(i)+"\n");
		output.flush();
	    }
	    System.out.println("---------------------------");
	    output.write("---------------------------\n");
	    output.flush();
	}catch(IOException e){
		System.out.println("Problème lors de l'écriture dans le fichier "+nomFichier);
		System.exit(0);
	    }

	ArrayList<Integer> vainqueurs=new ArrayList<Integer>();
	vainqueurs.add(0);
	
	for(int i=1;i<la.getNbJoueur();i++){
	    if(la.getJoueur(i).getNbPoints()==la.getJoueur(vainqueurs.get(0)).getNbPoints())
		vainqueurs.add(i);
	    else
		if(la.getJoueur(i).getNbPoints()>la.getJoueur(vainqueurs.get(0)).getNbPoints()){
		    vainqueurs.clear();
		    vainqueurs.add(i);
		}
	}
	
	// ajout du vainqueur dans les logs
	if(vainqueurs.size()==1){
	    System.out.println("VAINQUEUR : "+la.getJoueur(vainqueurs.get(0)).getNom()+" avec "+la.getJoueur(vainqueurs.get(0)).getNbPoints()+" points");
	    try{
		output.write(getDate()+" -  VAINQUEUR : "+la.getJoueur(vainqueurs.get(0)).getNom()+" avec "+la.getJoueur(vainqueurs.get(0)).getNbPoints()+" points\n");
		output.flush();
	    }catch(IOException e){
		System.out.println("Problème lors de l'écriture dans le fichier "+nomFichier);
		System.exit(0);
	    }
	}
	else{
	    System.out.println("NOUS AVONS DES EXAEQUO :");
	    for(int i=0;i<vainqueurs.size();i++)
		System.out.println(la.getJoueur(vainqueurs.get(i)).getNom()+" avec "+la.getJoueur(vainqueurs.get(i)).getNbPoints()+" points");	
	}

	if(screenshot)
	    f.snapShot(cheminEnregistrerImage+"/"+getNomFichier(dateLancement,tour)+".jpg");


	try{
	    output.close();
	}catch(IOException e){
	    System.out.println("Problème lors de la fermeture du fichier "+nomFichier);
	    System.exit(0);
	}
	f.fermer();
	
	//try{Thread.sleep(10000);}catch(java.lang.Exception e){}
	}
	//System.exit(0);
    }
}

