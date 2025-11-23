class Joueur{
    private int posX=0,posY=0;
    private String nom;
    private int nbPoints=0;
    private int nbSaut=0;
    private int nb3Pas=0;

    public Joueur(){
	posX=0;
	posY=0;
	nom="Inconnu";
	nbPoints=0;
	nbSaut=0;
	nb3Pas=0;
    }

    public Joueur(int pposX, int pposY, String nnom){
	posX=pposX;
	posY=pposY;
	nom=new String(nnom);
	nbPoints=0;
	nbSaut=0;
	nb3Pas=0;
    }

    public Joueur(Joueur j){
	posX=j.posX;
	posY=j.posY;
	nom=new String(j.nom);
	nbPoints=j.nbPoints;
	nbSaut=j.nbSaut;
	nb3Pas=j.nb3Pas;
    }

    public void translate(int deltaX, int deltaY){
	posX+=deltaX;
	posY+=deltaY;
    }

    public int getPosX(){
	return posX;
    }

    public int getPosY(){
	return posY;
    }

    public void setPosX(int pposX){
	posX=pposX;
    }

    public void setPosY(int pposY){
	posY=pposY;
    }

    public String getNom(){
	return nom;
    }

    public int getNbPoints(){
	return nbPoints;
    }

    public void ajouterBonus(String bonus){
	if(bonus.equals("Bp"))
	    nb3Pas++;
	else{
	    if(bonus.equals("Bs"))
		nbSaut++;
	    else
		throw new java.lang.RuntimeException("Bonus "+bonus+" inconnu");
	}
    }

    public void ajouter3Pas(){
	nb3Pas++;
    }

    public void ajouterSaut(){
	nbSaut++;
    }

    public void enlever3Pas(){
	nb3Pas--;
    }

    public void enleverSaut(){
	nbSaut--;
    }

    public void ajouterPoints(int pt){
	nbPoints+=pt;
    }

    public int getNbSaut(){
	return nbSaut;
    }

    public int getNb3Pas(){
	return nb3Pas;
    }

    public String toString(){
	String retour = "Joueur "+nom+" en ("+posX+","+posY+") a "+nbPoints;
	if(nbPoints==0)
	    retour=retour.concat(" point et possède "+nb3Pas);
	else
	    retour=retour.concat(" points et possède "+nb3Pas);
	if(nb3Pas<2)
	    retour=retour.concat(" bonus 3 pas et "+nbSaut);
	else
	    retour=retour.concat(" bonus 3 pas et "+nbSaut);
	if(nbSaut<2)
	    retour=retour.concat(" bonus saut");
	else
	    retour=retour.concat(" bonus saut");
	
	return retour;
    }
}
