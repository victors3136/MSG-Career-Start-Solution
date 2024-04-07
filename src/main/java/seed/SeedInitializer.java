package seed;

import repository.AccountsRepository;

import java.util.concurrent.atomic.AtomicBoolean;

import static seed.AccountsSeedData.checkingAccountA;
import static seed.AccountsSeedData.checkingAccountB;
import static seed.AccountsSeedData.checkingAccountC;
import static seed.AccountsSeedData.checkingAccountD;
import static seed.AccountsSeedData.savingsAccountA;
import static seed.AccountsSeedData.savingsAccountB;

public class SeedInitializer {
    private static final AtomicBoolean initialized = new AtomicBoolean(false);

    public static void seedData() {
        if (initialized.get()) {
            return;
        }
        initialized.set(true);
        System.out.println("[Seeder] -------------Seeding data----------------\n");
        AccountsRepository.INSTANCE.add(savingsAccountA.getId(), savingsAccountA);
        AccountsRepository.INSTANCE.add(savingsAccountB.getId(), savingsAccountB);
        AccountsRepository.INSTANCE.add(checkingAccountA.getId(), checkingAccountA);
        AccountsRepository.INSTANCE.add(checkingAccountB.getId(), checkingAccountB);
        AccountsRepository.INSTANCE.add(checkingAccountC.getId(), checkingAccountC);
        AccountsRepository.INSTANCE.add(checkingAccountD.getId(), checkingAccountD);
    }

}
