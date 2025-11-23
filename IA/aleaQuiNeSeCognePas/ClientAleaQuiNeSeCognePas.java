import java.io.*;
import java.net.*;
import java.util.Random;

public class ClientAleaQuiNeSeCognePas{
	
    public static void main(String[] args){
	if(args.length!=3){
	    System.out.println("Il faut 3 arguments : l'adresse ip du serveur, le port et le nom d'équipe");
	    System.exit(0);
	}
	Random rand=new Random();
	Labyrinthe la=null;
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
			
	    while(!fin){
		String msg = bf.readLine();
		
		System.out.println("Message recu : "+msg);
		System.out.println();
		fin=msg.equals("FIN");

		if(!fin){
		    if(la==null)
			la = new Labyrinthe(msg);
		    String[] infos=msg.split("/");
		    String[] positions=infos[2].split("-");
		    String[] maposition=positions[numJoueur+1].split(",");
		    int x=Integer.parseInt(maposition[0]);
		    int y=Integer.parseInt(maposition[1]);
		    
		    boolean coupPossible=false;
		    while(!coupPossible){
			switch(rand.nextInt(4)){
			case 0:
			    msg="N";
			    if(la.getXY(x,y-1).getType()!=Case.MUR)
				coupPossible=true;
			    break;
			case 1:
			    msg="S";
			    if(la.getXY(x,y+1).getType()!=Case.MUR)
				coupPossible=true;
			    break;
			case 2:
			    msg="E";
			    if(la.getXY(x+1,y).getType()!=Case.MUR)
				coupPossible=true;
			    break;
			default:
			    msg="O";
			    if(la.getXY(x-1,y).getType()!=Case.MUR)
				coupPossible=true;
			}
		    }
		
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
