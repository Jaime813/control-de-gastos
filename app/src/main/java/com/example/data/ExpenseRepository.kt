package com.example.data

import android.content.Context
import com.example.R
import com.example.model.Expense
import com.example.model.PaymentMethod
import com.example.model.UserSettings
import com.example.util.DateFormatUtils
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ExpenseRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireMatchedUserId(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
        check(!uid.isNullOrBlank()) {
            "El usuario debe iniciar sesión con Google antes de acceder a Firestore."
        }
        check(uid == expectedUserId) {
            "La sesión cambió; operación cancelada."
        }
        return uid
    }

    fun observeExpenses(userId: String): Flow<List<Expense>> = flow {
        if (userId.isBlank() || auth.currentUser?.uid != userId) {
            emit(emptyList())
            return@flow
        }
        val path = "users/$userId/expenses"
        emitAll(
            db.collection("users")
                .document(userId)
                .collection("expenses")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Expense::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<Expense> { it.dateKey }
                            .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeUserSettings(userId: String): Flow<UserSettings> = flow {
        if (userId.isBlank() || auth.currentUser?.uid != userId) {
            emit(UserSettings(id = userId, userId = userId))
            return@flow
        }
        val path = "users/$userId"
        emitAll(
            db.collection("users")
                .document(userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        UserSettings::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ) ?: UserSettings(id = userId, userId = userId)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addExpense(
        userId: String,
        title: String,
        amount: Double,
        categoryId: String,
        categoryLabel: String,
        dateKey: String,
        note: String?,
        paymentMethod: PaymentMethod = PaymentMethod.CASH,
        cardName: String? = null,
        installments: Int = 1,
        creditSettled: Boolean = false,
    ): Result<Unit> {
        var docPath = "users/$userId/expenses"
        return try {
            val uid = requireMatchedUserId(userId)
            require(amount > 0.0 && amount <= 10_000_000.0) {
                "El monto debe ser mayor a 0 y menor a 10,000,000."
            }
            require(DateFormatUtils.isValidDateKey(dateKey)) {
                "Fecha inválida."
            }
            val cleanCategoryId = categoryId.trim().take(40).ifBlank { "OTHER" }
            val cleanTitle = title.trim().ifBlank { categoryLabel.ifBlank { "Gasto" } }.take(80)
            val cleanNote = note?.trim()?.takeIf { it.isNotEmpty() }?.take(160)
            val cleanCardName = if (paymentMethod == PaymentMethod.CREDIT_CARD) {
                cardName?.trim()?.takeIf { it.isNotEmpty() }?.take(40)
            } else null
            val validInstallments = if (paymentMethod == PaymentMethod.CREDIT_CARD) {
                installments.coerceIn(1, 60)
            } else 1
            val validSettled = paymentMethod == PaymentMethod.CREDIT_CARD && creditSettled
            val monthKey = dateKey.take(7)

            val docRef = db.collection("users")
                .document(uid)
                .collection("expenses")
                .document()
            docPath = docRef.path

            val payload = mapOf(
                "userId" to uid,
                "title" to cleanTitle,
                "amount" to amount,
                "category" to cleanCategoryId,
                "dateKey" to dateKey,
                "monthKey" to monthKey,
                "note" to cleanNote,
                "paymentMethod" to paymentMethod.name,
                "cardName" to cleanCardName,
                "installments" to validInstallments,
                "creditSettled" to validSettled,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            ).filterValues { it != null }

            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateExpense(
        userId: String,
        expenseId: String,
        title: String,
        amount: Double,
        categoryId: String,
        categoryLabel: String,
        dateKey: String,
        note: String?,
        paymentMethod: PaymentMethod = PaymentMethod.CASH,
        cardName: String? = null,
        installments: Int = 1,
        creditSettled: Boolean = false,
    ): Result<Unit> {
        var docPath = "users/$userId/expenses/$expenseId"
        return try {
            val uid = requireMatchedUserId(userId)
            require(expenseId.isNotBlank()) { "El ID del gasto no puede estar vacío." }
            require(amount > 0.0 && amount <= 10_000_000.0) {
                "El monto debe ser mayor a 0 y menor a 10,000,000."
            }
            require(DateFormatUtils.isValidDateKey(dateKey)) {
                "Fecha inválida."
            }
            val cleanCategoryId = categoryId.trim().take(40).ifBlank { "OTHER" }
            val cleanTitle = title.trim().ifBlank { categoryLabel.ifBlank { "Gasto" } }.take(80)
            val cleanNote = note?.trim()?.take(160) ?: ""
            val cleanCardName = if (paymentMethod == PaymentMethod.CREDIT_CARD) {
                cardName?.trim()?.take(40) ?: ""
            } else ""
            val validInstallments = if (paymentMethod == PaymentMethod.CREDIT_CARD) {
                installments.coerceIn(1, 60)
            } else 1
            val validSettled = paymentMethod == PaymentMethod.CREDIT_CARD && creditSettled
            val monthKey = dateKey.take(7)

            val docRef = db.collection("users")
                .document(uid)
                .collection("expenses")
                .document(expenseId)
            docPath = docRef.path

            docRef.update(
                mapOf(
                    "title" to cleanTitle,
                    "amount" to amount,
                    "category" to cleanCategoryId,
                    "dateKey" to dateKey,
                    "monthKey" to monthKey,
                    "note" to cleanNote,
                    "paymentMethod" to paymentMethod.name,
                    "cardName" to cleanCardName,
                    "installments" to validInstallments,
                    "creditSettled" to validSettled,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun setCreditSettled(
        userId: String,
        expenseId: String,
        settled: Boolean,
    ): Result<Unit> {
        var docPath = "users/$userId/expenses/$expenseId"
        return try {
            val uid = requireMatchedUserId(userId)
            require(expenseId.isNotBlank()) { "El ID del gasto no puede estar vacío." }
            val docRef = db.collection("users")
                .document(uid)
                .collection("expenses")
                .document(expenseId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "creditSettled" to settled,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteExpense(userId: String, expenseId: String): Result<Unit> {
        var docPath = "users/$userId/expenses/$expenseId"
        return try {
            val uid = requireMatchedUserId(userId)
            require(expenseId.isNotBlank()) { "El ID del gasto no puede estar vacío." }
            val docRef = db.collection("users")
                .document(uid)
                .collection("expenses")
                .document(expenseId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveUserSettings(
        userId: String,
        dailyBudget: Double,
        currencySymbol: String,
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireMatchedUserId(userId)
            require(dailyBudget in 0.0..10_000_000.0) {
                "El presupuesto diario debe estar entre 0 y 10,000,000."
            }
            val cleanCurrency = currencySymbol.trim().ifBlank { "$" }.take(5)
            val docRef = db.collection("users").document(uid)
            val exists = docRef.get().await().exists()
            if (exists) {
                docRef.update(
                    mapOf(
                        "dailyBudget" to dailyBudget,
                        "currencySymbol" to cleanCurrency,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "dailyBudget" to dailyBudget,
                        "currencySymbol" to cleanCurrency,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }
}
