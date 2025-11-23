import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.ArrayList;

public class Parcours{
	
    public static void main(String[] args){
	if(args.length!=3){
	    System.out.println("Il faut 3 arguments : l'adresse ip du serveur, le port et le nom d'équipe");
	    System.exit(0);
	}
	Random rand=new Random();
	Labyrinthe la=null;
	ArrayList<Integer> caseVisitee = new ArrayList<Integer>();
	int numJoueur;
		
	try{
	    Socket s = new Socket(args[0],Integer.parseInt(args[1]));
	    boolean fin=false;
						
	    // ecriture
	    OutputStream os  = s.getOutputStream();
	    PrintWriter pw = new PrintWriter(os);
	    //lecture
	    InputStream is = s.getInputStream();
	    BufferedReader bf = new BufferedReader(
						   new InputStreamReader(is));

	    pw.println(args[2]);
	    pw.flush();
	    
	    String numJoueurS = bf.readLine();

	    System.out.println("Numero de joueur : "+numJoueurS);
	    numJoueur=Integer.parseInt(numJoueurS);

	    int numTour=0;
			
	    while(!fin){
		numTour++;
		String msg = bf.readLine();

		System.out.println("Message reçu : "+msg);
		System.out.println();
		fin=msg.equals("FIN");

		if(!fin){
		
		    if(la==null)
			la = new Labyrinthe(msg);
		    //récupérer notre position;
		    String[] infos=msg.split("/");
		    String[] positions=infos[2].split("-");
		    String[] maposition=positions[numJoueur+1].split(",");
		    int x=Integer.parseInt(maposition[0]);
		    int y=Integer.parseInt(maposition[1]);
		    int numCase=la.getIndex(x,y);
		    caseVisitee.add(numCase);
		
		    String possibilite="";
		    //Si on peut marcher sur la case
		    if(la.marchable(x-1,y)){
			//On verifie qu'on ne l'a pas déjà visiter
			if(!caseVisitee.contains(la.getIndex(x-1,y)))
			    possibilite=possibilite.concat("O");
		    }
		    if(la.marchable(x+1,y)){
			//On verifie qu'on ne l'a pas déjà visiter
			if(!caseVisitee.contains(la.getIndex(x+1,y)))
			    possibilite=possibilite.concat("E");
		    }
		    if(la.marchable(x,y-1)){
			//On verifie qu'on ne l'a pas déjà visiter
			if(!caseVisitee.contains(la.getIndex(x,y-1)))
			    possibilite=possibilite.concat("N");
		    }
		    if(la.marchable(x,y+1)){
			//On verifie qu'on ne l'a pas déjà visiter
			if(!caseVisitee.contains(la.getIndex(x,y+1)))
			    possibilite=possibilite.concat("S");
		    }
		
		
		
		    // TODO - Si on a aucune possibilite, c'est qu'on est dans un cul de sac, il faut revenir en arriere
		    // Il faut trouver la bonne case.
		    if(possibilite.equals("")){
			int[] indexcase={caseVisitee.size()+1,caseVisitee.size()+1,caseVisitee.size()+1,caseVisitee.size()+1};
			if(la.marchable(x-1,y)){
			    //On verifie qu'on ne l'a pas déjà visiter
			    for(int i=0;i<caseVisitee.size();i++)
				if(caseVisitee.get(i)==la.getIndex(x-1,y))
				    indexcase[0]=i;
			}
			if(la.marchable(x+1,y)){
			    //On verifie qu'on ne l'a pas déjà visiter
			    for(int i=0;i<caseVisitee.size();i++)
				if(caseVisitee.get(i)==la.getIndex(x+1,y))
				    indexcase[1]=i;
			}
			if(la.marchable(x,y-1)){
			    //On verifie qu'on ne l'a pas déjà visiter
			    for(int i=0;i<caseVisitee.size();i++)
				if(caseVisitee.get(i)==la.getIndex(x,y-1))
				    indexcase[2]=i;
		    
			}
			if(la.marchable(x,y+1)){
			    //On verifie qu'on ne l'a pas déjà visiter
			    for(int i=0;i<caseVisitee.size();i++)
				if(caseVisitee.get(i)==la.getIndex(x,y+1))
				    indexcase[3]=i;
			}

			int indice=0;
			if(indexcase[1]<indexcase[0])
			    indice=1;
			if(indexcase[2]<indexcase[indice])
			    indice=2;
			if(indexcase[3]<indexcase[indice])
			    indice=3;

			if(indice==0)
			    possibilite="O";
			if(indice==1)
			    possibilite="E";
			if(indice==2)
			    possibilite="N";
			if(indice==3)
			    possibilite="S";
		    
		    }
		    msg=""+possibilite.charAt(rand.nextInt(possibilite.length()));
		    System.out.println("Envoi de : "+msg);

		    /*try{
			if(numTour%5==4)
			    Thread.sleep(4000);
			else
			    Thread.sleep(1000);
			    }catch(Exception e){}*/
		
		    pw.println(msg);
		    pw.flush();
		}
		
	    }
	    s.close();
	    
	}catch(Exception e){
	    System.err.println(e);
	    e.printStackTrace();
	}
    }
	
}
