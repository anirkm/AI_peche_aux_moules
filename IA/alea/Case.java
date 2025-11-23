class Case{
    public static final int SOL = 0; // peut marcher dessus
    public static final int MUR = 1; // on ne peut pas le franchir sauf en utilisant un bonus saut
    public static final int BONUSSAUT = 2; // permet d'avancer de deux cases dans une direction et même de sauter par dessus un mur
    public static final int BONUS3PAS = 3; // permet de jouer 3 coups d'affilée
    public static final int TRESOR = 4; // points à récupérer

    // ATTENTION ! L'ordre doit correspondre à ce qui est déclarer au dessus !
    private static String[] listeType = {"SOL", "MUR", "BONUSSAUT", "BONUS3PAS", "TRESOR"};

    public int type=Case.MUR;
    public int pointRapporte=0;

    public Case(){
	type = Case.MUR;
	pointRapporte=0;
    }
    
    public Case(int ttype, int ppointRapporte){
	setType(ttype);
	setPointRapporte(ppointRapporte);
    }
    
    public Case(String ttype, int ppointRapporte){
	setType(ttype);
	setPointRapporte(ppointRapporte);
    }

    public Case(Case c){
	type=c.type;
	pointRapporte=c.pointRapporte;
    }

    public int getType(){
	return type;
    }

    public int getPointRapporte(){
	return pointRapporte;
    }

    public void setType(int ttype){
	if(ttype<0 || ttype>listeType.length)
	    throw new java.lang.RuntimeException("Classe Case\n--> Type de case inconnu");
	type=ttype;
    }

    public void setType(String ttype){
	type=-1;
	for(int i=0;i<listeType.length;i++)
	    if(ttype.equals(listeType[i])){
		type=i;
	    }
	if(type==-1)
	    throw new java.lang.RuntimeException("Classe Case\n--> Type de case inconnu");
    }

    public void setPointRapporte(int ppointRapporte){
	if(ppointRapporte<0)
	    throw new java.lang.RuntimeException("Classe Case\n--> Nombre de point négatif");
	if(type!=Case.TRESOR && ppointRapporte>0)
	    throw new java.lang.RuntimeException("Classe Case\n--> Une case qui n'est pas de type tresor ne peut pas rapporter de points");
	pointRapporte=ppointRapporte;
    }

    public String toString(){
	if(type==Case.MUR)
	    return "Mu";
	if(type==Case.SOL)
	    return "So";
	if(type==Case.BONUSSAUT)
	    return "Bs";
	if(type==Case.TRESOR)
	    return ""+pointRapporte;
	return "Bp";
    }
}
