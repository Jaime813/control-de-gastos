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
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read or write user settings or expenses", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("expenses").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("expenses").doc("exp_1").set({
      userId: ALICE_UID,
      title: "Café",
      amount: 45.0,
      category: "FOOD",
      dateKey: "2026-11-19",
      monthKey: "2026-11",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Authenticated owner: valid UserSettings create and update succeed; ghost fields fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      dailyBudget: 500.0,
      currencySymbol: "COP $",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      dailyBudget: 650.0,
      currencySymbol: "€",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertFails(aliceDb.collection("users").get());
});

test("Authenticated owner: valid Expense and credit card purchases create, update, and reject invalid bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Regular cash expense
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("expenses").doc("exp_1").set({
      userId: ALICE_UID,
      title: "Almuerzo del día",
      amount: 120.5,
      category: "FOOD",
      dateKey: "2026-11-19",
      monthKey: "2026-11",
      paymentMethod: "CASH",
      installments: 1,
      creditSettled: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Credit card purchase in installments
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("expenses").doc("exp_credit").set({
      userId: ALICE_UID,
      title: "Mercado del mes",
      amount: 360000,
      category: "HOME",
      dateKey: "2026-11-19",
      monthKey: "2026-11",
      paymentMethod: "CREDIT_CARD",
      cardName: "Visa Nu",
      installments: 3,
      creditSettled: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Toggle creditSettled partial update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("expenses").doc("exp_credit").update({
      creditSettled: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject invalid paymentMethod or out-of-range installments
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("expenses").doc("exp_bad_pm").set({
      userId: ALICE_UID,
      title: "Malo",
      amount: 50,
      category: "OTHER",
      dateKey: "2026-11-19",
      monthKey: "2026-11",
      paymentMethod: "CRYPTO",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("expenses").doc("exp_bad_inst").set({
      userId: ALICE_UID,
      title: "Cuotas malas",
      amount: 50,
      category: "OTHER",
      dateKey: "2026-11-19",
      monthKey: "2026-11",
      paymentMethod: "CREDIT_CARD",
      installments: 100,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Mirror Kotlin query
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("expenses").get()
  );

  // Partial update with note
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("expenses").doc("exp_1").update({
      title: "Almuerzo ejecutivo",
      amount: 145.0,
      category: "FOOD",
      dateKey: "2026-11-19",
      monthKey: "2026-11",
      note: "Con café incluido",
      paymentMethod: "CASH",
      cardName: "",
      installments: 1,
      creditSettled: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject adding @DocumentId `id` field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("expenses").doc("exp_2").set({
      id: "exp_2",
      userId: ALICE_UID,
      title: "Taxi",
      amount: 80.0,
      category: "TRANSPORT",
      dateKey: "2026-11-19",
      monthKey: "2026-11",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Delete succeeds for owner
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("expenses").doc("exp_1").delete()
  );
});

test("Cross-user isolation: Alice cannot read, create, update, or delete Bob's data", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      dailyBudget: 400.0,
      currencySymbol: "$",
      createdAt: new Date(Date.now() - 10000),
      updatedAt: new Date(Date.now() - 10000),
    });
    await db.collection("users").doc(BOB_UID).collection("expenses").doc("bob_exp").set({
      userId: BOB_UID,
      title: "Gasto de Bob",
      amount: 99.0,
      category: "HOME",
      dateKey: "2026-11-19",
      monthKey: "2026-11",
      createdAt: new Date(Date.now() - 10000),
      updatedAt: new Date(Date.now() - 10000),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("expenses").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("expenses").doc("bob_exp").get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("expenses").doc("bob_exp").update({
      title: "Hack",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("expenses").doc("bob_exp").delete()
  );
});
