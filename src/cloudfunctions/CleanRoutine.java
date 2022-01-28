package cloudfunctions;

import database.social.User;

import java.util.Calendar;
import java.util.Iterator;

import database.Database;
import server.ServerMain;

public class CleanRoutine implements Runnable{
    private Database db;
    private int timeout;

    public CleanRoutine(Database db, int timeout) {
        this.db = db;
        this.timeout = timeout;
    }

    @Override
    public void run() {
        // cleanup social.loggedUsers removing unactive users
        while(!ServerMain.quit && !Thread.currentThread().isInterrupted()){
            try {
                Thread.sleep(timeout);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Iterator<String> it = db.getSocialInstance().getLoggedUsers().iterator();
            while(it.hasNext()){
                String uname = it.next();
                try{
                    User u = new User( db.getUser(uname), db );
                    if(u!=null && u.getLast_session().getTime()+(3*this.timeout) < 
                            Calendar.getInstance().getTime().getTime()){
                        // remove user
                        it.remove();
                        // remove from rmi callback registered set
                        // these should be cloud function responses to server. keep it simple
                        server.ServerMain.serverRMIService.sendLogoutNotification(uname);
                        server.ServerMain.serverRMIService.safeUnregisterForCallback(uname);
                    }
                }catch(NullPointerException e){
                    // not possible here
                    // working on a user which is now deleted from another thread
                    continue;
                }
            }
        }
    }
    
}
