package services;

import domain.*;
import repository.AccountsRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

import static utils.MoneyUtils.convert;

public class TransactionManagerService {

    private static class TransactionValidator {
        private static boolean sourceHasEnoughFundsToCompleteTransaction(AccountModel sourceAccount, AccountModel destinationAccount, MoneyModel transactedSum) {
            MoneyModel fundsInSourceAccount = sourceAccount.getBalance();
            CurrencyType sourceCurrency = sourceAccount.getBalance().getCurrency();
            CurrencyType destinationCurrency = destinationAccount.getBalance().getCurrency();
            CurrencyType transactionCurrency = transactedSum.getCurrency();
            ///A transaction final amount must be in the same currency as the target account (consider currency conversion).
            if (sourceCurrency != destinationCurrency) {
                fundsInSourceAccount = convert(fundsInSourceAccount, destinationCurrency);
            }
            if (transactionCurrency != destinationCurrency) {
                transactedSum = convert(transactedSum, destinationCurrency);
            }
            return (fundsInSourceAccount.getAmount() >= transactedSum.getAmount());
        }

        private static boolean checkDailyLimitForTypeOfAccount(CheckingAccountModel account, MoneyModel amount, LocalDate dateOfTransaction, Predicate<TransactionModel> filterCriteria) {
            double amountConversion = convert(amount, account.getBalance().getCurrency()).getAmount();
            return account
                    .getTransactions()
                    .stream()
                    /// Get today's transactions
                    .filter(transaction -> transaction.getTimestamp().atStartOfDay().equals(dateOfTransaction.atStartOfDay()))
                    /// Filter by provided criteria
                    .filter(filterCriteria)
                    /// Convert all withdrawals to the account currency
                    .map(transaction -> convert(transaction.getAmount(), account.getBalance().getCurrency()))
                    /// Get actual amount of money
                    .map(MoneyModel::getAmount)
                    /// Sum
                    .reduce(0., Double::sum)
                    /// Add the sum for the requested withdrawal
                    + amountConversion
                    < account.getAssociatedCard().getDailyWithdrawalLimit();
        }

        private static boolean checkingAccountHasNotReachedDailyWithdrawalLimit(CheckingAccountModel account, MoneyModel amount, LocalDate dateOfTransaction) {
            return checkDailyLimitForTypeOfAccount(account, amount, dateOfTransaction, TransactionModel::isWithdrawal);
        }

        private static boolean checkingAccountHasNotReachedDailyTransactionLimit(CheckingAccountModel account, MoneyModel amount, LocalDate dateOfTransaction) {
            return checkDailyLimitForTypeOfAccount(account, amount, dateOfTransaction, TransactionModel::isTransfer);
        }

        private static boolean validateSufficientBalance(AccountModel sourceAccount, AccountModel destinationAccount, MoneyModel transactedSum) {
            return sourceHasEnoughFundsToCompleteTransaction(sourceAccount, destinationAccount, transactedSum);
        }

        private static boolean validateWithdrawal(AccountModel account, MoneyModel transactedSum, LocalDate dateOfWithdrawal) {
            /// first check if there are enough funds
            return validateSufficientBalance(account, account, transactedSum) &&
                    /// If the account is checking (and has an associated card)
                    (!(account instanceof CheckingAccountModel checkingAccount) || (
                            /// check if card is valid
                            checkingAccount.getAssociatedCard().isActive() &&
                                    /// check if card is in its valid period
                                    checkingAccount.getAssociatedCard().getIssueDate().isBefore(LocalDate.now()) &&
                                    checkingAccount.getAssociatedCard().getExpirationDate().isAfter(LocalDate.now()) &&
                                    /// and hasn't reached withdrawal limit
                                    checkingAccountHasNotReachedDailyWithdrawalLimit(checkingAccount, transactedSum, dateOfWithdrawal)));
        }

        private static boolean validateTransfer(AccountModel source, AccountModel destination, MoneyModel transactedSum, LocalDate dateOfTransaction) {
            return validateSufficientBalance(source, destination, transactedSum) &&
                    /// If the account is checking (and has an associated card)
                    (!(source instanceof CheckingAccountModel checkingAccount) ||
                            /// check if card hasn't reached transaction limit
                            checkingAccountHasNotReachedDailyTransactionLimit(checkingAccount, transactedSum, dateOfTransaction));
        }

        static public boolean validate(AccountModel source, AccountModel dest, MoneyModel transactedSum, LocalDate dateOfTransaction) {
            return source.equals(dest) ? validateWithdrawal(source, transactedSum, dateOfTransaction) :
                    validateTransfer(source, dest, transactedSum, dateOfTransaction);
        }
    }

    private static void updateAccountBalanceAndLogTransaction(AccountModel account, double sum, TransactionModel transaction) {
        account.getBalance().setAmount(account.getBalance().getAmount() + sum);
        account.getTransactions().add(transaction);
    }

    public static TransactionModel transfer(String fromAccountId, String toAccountId, MoneyModel value) {
        byte SOURCE_COEFFICIENT = -1;
        byte DESTINATION_COEFFICIENT = 1;
        AccountModel fromAccount = AccountsRepository.INSTANCE.get(fromAccountId);
        AccountModel toAccount = AccountsRepository.INSTANCE.get(toAccountId);

        if (fromAccount == null || toAccount == null) {
            throw new RuntimeException("Specified account does not exist");
        }
        LocalDate dateOfTransaction = LocalDate.now();
        if (!TransactionValidator.validate(fromAccount, toAccount, value, dateOfTransaction)) {
            throw new RuntimeException("Validation of transaction failed during initialization");
        }
        /// Now we are sure that this transaction would be valid if executed, so we construct the object
        TransactionModel transaction = new TransactionModel(UUID.randomUUID(), fromAccountId, toAccountId, value, dateOfTransaction);

        ///A transaction final amount must be in the same currency as the target account
        double transactionSumOnSourceSide = convert(value, fromAccount.getBalance().getCurrency()).getAmount();
        double transactionSumOnDestSide = convert(value, toAccount.getBalance().getCurrency()).getAmount();
        updateAccountBalanceAndLogTransaction(fromAccount, SOURCE_COEFFICIENT * transactionSumOnSourceSide, transaction);
        updateAccountBalanceAndLogTransaction(toAccount, DESTINATION_COEFFICIENT * transactionSumOnDestSide, transaction);
        return transaction;
    }

    public static TransactionModel withdraw(String accountId, MoneyModel amount) {
        return transfer(accountId, accountId, amount);
    }

    public static MoneyModel checkFunds(String accountId) {
        if (AccountsRepository.INSTANCE.doesNotHave(accountId)) {
            throw new RuntimeException("Specified account does not exist");
        }
        return AccountsRepository.INSTANCE.get(accountId).getBalance();
    }

    public static List<TransactionModel> retrieveTransactions(String accountId) {
        if (AccountsRepository.INSTANCE.doesNotHave(accountId)) {
            throw new RuntimeException("Specified account does not exist");
        }
        return new ArrayList<>(AccountsRepository.INSTANCE.get(accountId).getTransactions());
    }
}

