package cloudfunctions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap.KeySetView;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import database.Database;
import database.social.Post;
import database.social.User;
import exceptions.ResourceNotFoundException;
import server.ServerMain;

public class RewardCalculator {
    /**
     * Calculate user rewards, using 3 maps of new post iterations since the last 
     * run of the calculator.
     * 
     * post.postAge = rewardLifeIterations (now) - rewardLifeIterations at post creation
     */
    public static void calculate(Database db) {
        int rewardLifeIterations = db.updateRewardIterations();

        ConcurrentHashMap<Integer, KeySetView<String, Boolean>> newUpvotes;
        ConcurrentHashMap<Integer, KeySetView<String, Boolean>> newDownvotes; 
        synchronized(db.getRateObj()){
            newDownvotes = new ConcurrentHashMap<>(db.getNewDownvotes());
            newUpvotes = new ConcurrentHashMap<>(db.getNewUpvotes());
        }
        
        ConcurrentHashMap<Integer, KeySetView<String, Boolean>> newComments;
        synchronized(db.getCommentObj()){
            newComments = new ConcurrentHashMap<>(db.getNewComments());
        }


        ConcurrentHashMap<Integer, Post> posts = new ConcurrentHashMap<>(db.getPosts());
        ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>(db.getUsers());

        HashSet<Integer> modifiedPosts = new HashSet<>();
        modifiedPosts.addAll(newUpvotes.keySet());
        modifiedPosts.addAll(newDownvotes.keySet());
        modifiedPosts.addAll(newComments.keySet());

        System.out.println(modifiedPosts); 

        // foreach post with new interactions
        Iterator <Integer> it = modifiedPosts.iterator();
        while (it.hasNext()) {
            int id = it.next();
            if (posts.containsKey(id)) { // post still exists
                boolean anyUpvotes = newUpvotes.containsKey(id);
                boolean anyDownvotes = newDownvotes.containsKey(id);
                boolean anyComments = newComments.containsKey(id);

                // number of upvotes and downvotes
                int upvotes = anyUpvotes ? newUpvotes.get(id).size() : 0;
                int downvotes = anyDownvotes ? newDownvotes.get(id).size() : 0;

                // number of comments for each user who commented the post
                HashMap<String, Integer> user_to_comments_number_map = new HashMap<String, Integer>();
                if(anyComments){
                    user_to_comments_number_map = (HashMap<String, Integer>) newComments.get(id).stream()
                        .collect(Collectors.toMap(Function.identity(), val -> 1, Integer::sum));
                }

                // formula for each user
                var sumWrapper = new Object() {
                    double sum = 0.0;
                };
                // sum calculation
                user_to_comments_number_map.forEach((user, cp) -> {
                    sumWrapper.sum += 2 / (1 + Math.pow(Math.E, -(cp - 1)));
                });

                Post post = posts.get(id);
                double reward = (Math.log(Math.max(upvotes - downvotes, 0) + 1) + Math.log(sumWrapper.sum + 1))
                        / (rewardLifeIterations - post.getPostAge());

                
                // author reward
                if (users.containsKey(post.getAuthor())) {
                    try {
                        db.updateUserWallet(post.getAuthor(), reward*ServerMain.server_config.AUTHOR_PERCENTAGE); // simplified
                    } catch (ResourceNotFoundException e) {
                        // System.out.println(e.getMessage());
                        // e.printStackTrace();
                    }
                }

                // curator reward
                HashSet<String> empty = new HashSet<String>(); // null
                // get the users who interacted with the post
                HashSet<String> curators = (HashSet<String>) Stream.of(
                        anyUpvotes ? newUpvotes.get(id) : empty,
                        anyDownvotes ? newDownvotes.get(id) : empty,
                        anyComments ? newComments.get(id) : empty)
                        .flatMap(u -> u.stream())
                        .collect(Collectors.toSet());

                curators.stream().filter(u -> users.containsKey(u)).forEach((username) -> {
                    try {
                        db.updateUserWallet(username, reward / curators.size() * (1 - ServerMain.server_config.AUTHOR_PERCENTAGE));
                    } catch (ResourceNotFoundException e) {
                        // System.out.println(e.getMessage());
                        // e.printStackTrace();
                    }
                });

            }
            it.remove();
            db.removeIdFromNewUpvotes(id);
            db.removeIdFromNewDownvotes(id);
            db.removeIdFromNewComments(id);
        }
    }
}
