package social.password;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
/**
 * Serialize this means serialize every function inside every password.
 * Need static functions outside and this class will only call them from here
 * 
 */
@NoArgsConstructor
public @Data class Password implements java.io.Serializable, Comparable<Password>{
    private String hashedPwd;

    public Password(String password) throws NoSuchAlgorithmException, InvalidKeySpecException{
        this.hashedPwd = PasswordUtil.generateStrongPasswordHash(password);
    }

    public boolean passwordMatches(String password) 
                                    throws NoSuchAlgorithmException, InvalidKeySpecException{
        return PasswordUtil.validatePassword(password, hashedPwd);
    }

    @Override
    public int compareTo(Password o) {
        return 1;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((hashedPwd == null) ? 0 : hashedPwd.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Password other = (Password) obj;
        if (hashedPwd == null) {
            if (other.hashedPwd != null)
                return false;
        } else if (!hashedPwd.equals(other.hashedPwd))
            return false;
        return true;
    }

    
}
