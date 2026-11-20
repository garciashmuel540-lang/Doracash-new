const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

test("Unauthenticated users cannot read or write any user finance collection", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).collection("accounts").get());
  await assertFails(
    unauth.collection("users").doc(ALICE_UID).collection("accounts").doc("acc_1").set({
      userId: ALICE_UID,
      name: "Efectivo",
      accountType: "CASH",
      currency: "NIO",
      balance: 1000,
      iconKey: "cash",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("User profile: owner create, partial update, cross-user denial, and shadow field rejection", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Nobita",
      displayCurrency: "NIO",
      exchangeRate: 36.62,
      darkThemeMode: "SYSTEM",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).update({
      displayCurrency: "USD",
      exchangeRate: 36.7,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field
  await assertFails(
    alice.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject future timestamp
  await assertFails(
    alice.collection("users").doc(ALICE_UID).update({
      exchangeRate: 37,
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Cross-user isolation
  await assertFails(bob.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bob.collection("users").doc(ALICE_UID).update({
      displayName: "Hacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Accounts, Transactions, ScheduledBills, Budgets, Goals, Reconciliations: Kotlin-mirrored payloads succeed and invalid payloads fail", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);
  const aliceRef = alice.collection("users").doc(ALICE_UID);

  // Account
  await assertSucceeds(
    aliceRef.collection("accounts").doc("acc_cash").set({
      userId: ALICE_UID,
      name: "Efectivo en Cartera",
      accountType: "CASH",
      currency: "NIO",
      balance: 4850.5,
      iconKey: "cash",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    aliceRef.collection("accounts").doc("acc_cash").update({
      balance: 4200,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  // Reject @DocumentId `id` leakage
  await assertFails(
    aliceRef.collection("accounts").doc("acc_bad").set({
      id: "acc_bad",
      userId: ALICE_UID,
      name: "Bad",
      accountType: "CASH",
      currency: "NIO",
      balance: 100,
      iconKey: "cash",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Transaction (expense without toAccountId, and transfer with toAccountId)
  await assertSucceeds(
    aliceRef.collection("transactions").doc("tx_1").set({
      userId: ALICE_UID,
      txType: "EXPENSE",
      amount: 650,
      currency: "NIO",
      accountId: "acc_cash",
      categoryId: "comida",
      note: "Almuerzo",
      dateIso: "2026-11-19",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    aliceRef.collection("transactions").doc("tx_2").set({
      userId: ALICE_UID,
      txType: "TRANSFER",
      amount: 500,
      currency: "NIO",
      accountId: "acc_cash",
      toAccountId: "acc_bank",
      categoryId: "transfer",
      note: "Depósito",
      dateIso: "2026-11-19",
      createdAt: created,
      updatedAt: created,
    })
  );
  // Reject negative transaction amount
  await assertFails(
    aliceRef.collection("transactions").doc("tx_neg").set({
      userId: ALICE_UID,
      txType: "EXPENSE",
      amount: -10,
      currency: "NIO",
      accountId: "acc_cash",
      categoryId: "comida",
      note: "Bad",
      dateIso: "2026-11-19",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Scheduled Bill
  await assertSucceeds(
    aliceRef.collection("scheduledBills").doc("bill_1").set({
      userId: ALICE_UID,
      title: "Internet Fibra",
      amount: 1150,
      currency: "NIO",
      accountId: "acc_cash",
      categoryId: "servicios",
      frequency: "MONTHLY",
      startDateIso: "2026-11-20",
      paidDates: [],
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    aliceRef.collection("scheduledBills").doc("bill_1").update({
      paidDates: ["2026-11-20"],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Budget
  await assertSucceeds(
    aliceRef.collection("budgets").doc("bud_comida").set({
      userId: ALICE_UID,
      categoryId: "comida",
      limitNio: 6000,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Goal
  await assertSucceeds(
    aliceRef.collection("goals").doc("goal_1").set({
      userId: ALICE_UID,
      name: "Fondo Emergencia",
      targetNio: 30000,
      currentNio: 5000,
      iconKey: "shield",
      deadlineIso: "2027-06-30",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    aliceRef.collection("goals").doc("goal_1").update({
      currentNio: 6500,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reconciliation
  await assertSucceeds(
    aliceRef.collection("reconciliations").doc("rec_1").set({
      userId: ALICE_UID,
      dateIso: "2026-11-19",
      accountId: "acc_cash",
      currency: "NIO",
      systemBalance: 4850,
      countedBalance: 4800,
      difference: -50,
      balanceAdjusted: true,
      note: "Arqueo rápido",
      createdAt: created,
      updatedAt: created,
    })
  );

  // BankCard
  await assertSucceeds(
    aliceRef.collection("cards").doc("card_1").set({
      userId: ALICE_UID,
      alias: "Visa Oro LAFISE",
      bankName: "LAFISE",
      cardKind: "CREDIT",
      network: "VISA",
      last4: "4821",
      currency: "NIO",
      debtBalance: 3500,
      creditLimit: 25000,
      linkedAccountId: "acc_cash",
      autoDebitEnabled: true,
      autoDebitMode: "FULL_DEBT",
      autoDebitValue: 100,
      googleWalletLinked: true,
      cardFrozen: false,
      cutoffDay: 15,
      dueDay: 5,
      colorTheme: "dora_blue",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    aliceRef.collection("cards").doc("card_1").update({
      debtBalance: 1500,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  // Reject non-4-digit last4 (PCI / format safety)
  await assertFails(
    aliceRef.collection("cards").doc("card_bad").set({
      userId: ALICE_UID,
      alias: "Full PAN bad",
      bankName: "BAC",
      cardKind: "CREDIT",
      network: "VISA",
      last4: "4111222233334444",
      currency: "NIO",
      debtBalance: 0,
      creditLimit: 10000,
      linkedAccountId: "",
      autoDebitEnabled: true,
      autoDebitMode: "FULL_DEBT",
      autoDebitValue: 100,
      googleWalletLinked: false,
      cardFrozen: false,
      cutoffDay: 15,
      dueDay: 5,
      colorTheme: "obsidian",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Synced Device & App Release Metadata (multi-phone sync & update notifications)
  await assertSucceeds(
    aliceRef.collection("devices").doc("dev_pixel").set({
      userId: ALICE_UID,
      deviceName: "Google Pixel 8",
      versionName: "1.0",
      versionCode: 1,
      platform: "ANDROID",
      lastSeenIso: "2026-11-19",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    aliceRef.collection("devices").doc("dev_pixel").update({
      versionName: "1.1",
      versionCode: 2,
      lastSeenIso: "2026-11-20",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    aliceRef.collection("appMeta").doc("release").set({
      userId: ALICE_UID,
      latestVersionCode: 2,
      latestVersionName: "1.1",
      downloadUrl: "https://play.google.com/store/apps/details?id=com.aistudio.doracash.kmpzqx",
      releaseNotes: "Sincronización multi-teléfono",
      forceNotice: true,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    aliceRef.collection("appMeta").doc("release").update({
      latestVersionCode: 3,
      latestVersionName: "1.2",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  // Reject non-HTTPS insecure download URL
  await assertFails(
    aliceRef.collection("appMeta").doc("release").update({
      downloadUrl: "http://insecure.example.com/doracash.apk",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  // Reject writing arbitrary appMeta doc id other than 'release'
  await assertFails(
    aliceRef.collection("appMeta").doc("other").set({
      userId: ALICE_UID,
      latestVersionCode: 2,
      latestVersionName: "1.1",
      downloadUrl: "",
      releaseNotes: "",
      forceNotice: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Empty & populated list queries work for owner, fail for Bob
  await assertSucceeds(aliceRef.collection("accounts").get());
  await assertSucceeds(aliceRef.collection("transactions").get());
  await assertSucceeds(aliceRef.collection("cards").get());
  await assertSucceeds(aliceRef.collection("devices").get());
  await assertSucceeds(aliceRef.collection("appMeta").doc("release").get());
  await assertFails(bob.collection("users").doc(ALICE_UID).collection("accounts").get());
  await assertFails(bob.collection("users").doc(ALICE_UID).collection("transactions").get());
  await assertFails(bob.collection("users").doc(ALICE_UID).collection("scheduledBills").get());
  await assertFails(bob.collection("users").doc(ALICE_UID).collection("reconciliations").get());
  await assertFails(bob.collection("users").doc(ALICE_UID).collection("cards").get());
  await assertFails(bob.collection("users").doc(ALICE_UID).collection("devices").get());
  await assertFails(bob.collection("users").doc(ALICE_UID).collection("appMeta").doc("release").get());
});
