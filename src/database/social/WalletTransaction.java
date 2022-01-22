package database.social;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public @Data class WalletTransaction{
    private String date;
    private double amount;

    public WalletTransaction(String date, double amount) {
        this.date = date;
        this.amount = amount;
    }
}
