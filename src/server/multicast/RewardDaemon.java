package server.multicast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap.KeySetView;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import database.Database;
import exceptions.ResourceNotFoundException;
import server.ServerMain;
import server.util.Constants;
import server.util.Logger;
import social.Post;
import social.User;

public class RewardDaemon implements Runnable{
    private Logger LOGGER = new Logger(RewardDaemon.class.getName());
    private Database db;
    private int rewardPerformedIterations;
    private Thread nioThread;

    public RewardDaemon(Thread nioThread, Database db){
        this.nioThread = nioThread;
        this.db = db;
        this.rewardPerformedIterations = 0;
    }


    @Override
    public void run() {
        try (DatagramSocket skt = new DatagramSocket(Constants.MULTICAST_PORT)) {
            while (!Thread.currentThread().isInterrupted() && !ServerMain.quit ) {
                byte[] msg = "Rewards calculated".getBytes();
                try {
                    DatagramPacket datagram = new DatagramPacket(msg, msg.length,
                        InetAddress.getByName(Constants.MULTICAST_ADDRESS),
                        Constants.MULTICAST_PORT);
                    skt.send(datagram);
                } catch (UnknownHostException | SocketException e) {
                    // packet or socket error
                    e.printStackTrace();
                } catch (IOException ex) {
                    // comunication error
                    ex.printStackTrace();
                }
                rewardCalculator();
                try {
                    Thread.sleep(Constants.REWARD_TIMEOUT);
                } catch (InterruptedException ignored){}
            }
            try {
                nioThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //LOGGER.info("Performing last reward calculation");
            rewardCalculator(); // once more after all stopped
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return;
    }

    /**
     * Algorithm to calculate winsome's rewards based on post interactions.
     * Iterates on every modified post since the last time the algorithm ran,
     * avoiding unmodified posts;
     * to achieve this, it "consumes" three maps ( modifiedPostID ->
     * {usersWhoInteracted}).
     * 
     * To avoid iterating also on unmodified posts to increment their "age", we save
     * the number of times the reward algorithm has ran in the post
     * at its creation, so that post.age := {current reward algorithm iterations} -
     * {reward algorithm iterations at post's creation}
     */
    public void rewardCalculator() {
        rewardPerformedIterations++;

        ConcurrentHashMap<Integer, KeySetView<String, Boolean>> newUpvotes = db.getNewUpvotes();
        ConcurrentHashMap<Integer, KeySetView<String, Boolean>> newDownvotes = db.getNewDownvotes();
        ConcurrentHashMap<Integer, KeySetView<String, Boolean>> newComments = db.getNewComments();
        ConcurrentHashMap<Integer, Post> posts = db.getPosts();
        ConcurrentHashMap<String, User> users = db.getUsers();

        HashSet<Integer> modifiedPosts = new HashSet<>();
        modifiedPosts.addAll(newUpvotes.keySet());
        modifiedPosts.addAll(newDownvotes.keySet());
        modifiedPosts.addAll(newComments.keySet());

        // foreach post with new interactions
        modifiedPosts.forEach((id) -> {
            if (posts.containsKey(id)) { 
                // the post still exists
                boolean anyUpvotes = newUpvotes.containsKey(id);
                boolean anyDownvotes = newDownvotes.containsKey(id);
                boolean anyComments = newComments.containsKey(id);

                // get the number of upvotes and downvotes
                int upvotes = anyUpvotes ? newUpvotes.get(id).size() : 0;
                int downvotes = anyDownvotes ? newDownvotes.get(id).size() : 0;

                // count duplicates and get the number of comments for each "commenting" user, if any
                HashMap<String, Integer> nCommentsForEachUser = new HashMap<String, Integer>();
                if(newComments.containsKey(id)){
                    nCommentsForEachUser = (HashMap<String, Integer>) newComments.get(id).stream()
                        .collect(Collectors.toMap(Function.identity(), v -> 1, Integer::sum));
                }

                // now apply the formula to each user and calculate the sum
                var wrapper = new Object() {
                    Double sum = 0.0;
                };
                nCommentsForEachUser.forEach((user, cp) -> {
                    wrapper.sum += 2 / (1 + Math.pow(Math.E, -(cp - 1)));
                });

                Post post = posts.get(id);
                double reward = (Math.log(Math.max(upvotes - downvotes, 0) + 1) + Math.log(wrapper.sum + 1))
                        / (rewardPerformedIterations - post.getPostAge());

                
                // author reward
                if (users.containsKey(post.getAuthor())) {
                    try {
                        db.updateUserWallet(post.getAuthor(), reward*Constants.AUTHOR_PERCENTAGE);
                    } catch (ResourceNotFoundException e) {
                        LOGGER.warn(e.getMessage());
                        e.printStackTrace();
                        modifiedPosts.remove(id);
                        db.removeIdFromNewUpvotes(id);
                        db.removeIdFromNewDownvotes(id);
                        db.removeIdFromNewComments(id);
                        return; // skip to the next post
                    }
                }

                // curator reward
                HashSet<String> empty = new HashSet<String>(); // .flatMap handles null stream, but .of doesn't
                // get all the users who interacted with the post
                HashSet<String> curators = (HashSet<String>) Stream
                        .of(anyUpvotes ? newUpvotes.get(id) : empty, anyDownvotes ? newDownvotes.get(id) : empty,
                                anyComments ? new HashSet<String>(newComments.get(id)) : empty)
                        .flatMap(u -> u.stream())
                        .collect(Collectors.toSet());

                curators.stream().filter(u -> users.containsKey(u)).forEach((username) -> {
                    try {
                        db.updateUserWallet(username, reward / curators.size() * (1 - Constants.AUTHOR_PERCENTAGE));
                    } catch (ResourceNotFoundException e) {
                        LOGGER.warn(e.getMessage());
                        e.printStackTrace();
                        return; // skip to the next user
                    }

                    // we must use curators.size to avoid counting duplicates
                });

            }
            modifiedPosts.remove(id);
            db.removeIdFromNewUpvotes(id);
            db.removeIdFromNewDownvotes(id);
            db.removeIdFromNewComments(id);
        });
    }
    
}
