package com.example.lab_week_08

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.work.*
import com.example.lab_week_08.worker.FirstWorker
import com.example.lab_week_08.worker.SecondWorker
import com.example.lab_week_08.worker.ThirdWorker

class MainActivity : AppCompatActivity() {

    private val workManager by lazy { WorkManager.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // ✅ Request notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

        val id = "001"
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val firstRequest = OneTimeWorkRequest.Builder(FirstWorker::class.java)
            .setConstraints(constraints)
            .setInputData(getIdInputData(FirstWorker.INPUT_DATA_ID, id))
            .build()

        val secondRequest = OneTimeWorkRequest.Builder(SecondWorker::class.java)
            .setConstraints(constraints)
            .setInputData(getIdInputData(SecondWorker.INPUT_DATA_ID, id))
            .build()

        val thirdRequest = OneTimeWorkRequest.Builder(ThirdWorker::class.java)
            .setConstraints(constraints)
            .setInputData(getIdInputData(ThirdWorker.INPUT_DATA_ID, id))
            .build()

        // 🔄 WorkManager Sequence: First → Second → Third
        workManager.beginWith(firstRequest)
            .then(secondRequest)
            .then(thirdRequest)
            .enqueue()

        // Observe: FIRST WORKER
        workManager.getWorkInfoByIdLiveData(firstRequest.id).observe(this) { info ->
            if (info != null && info.state.isFinished) showResult("First process is done")
        }

        // Observe: SECOND WORKER
        workManager.getWorkInfoByIdLiveData(secondRequest.id).observe(this) { info ->
            if (info != null && info.state.isFinished) {
                showResult("Second process is done")
                startNotificationService(id)
            }
        }

        // Observe: THIRD WORKER
        workManager.getWorkInfoByIdLiveData(thirdRequest.id).observe(this) { info ->
            if (info != null && info.state.isFinished) {
                showResult("Third process is done")
                startSecondNotificationService(id)
            }
        }
    }

    private fun getIdInputData(idKey: String, idValue: String) =
        Data.Builder().putString(idKey, idValue).build()

    private fun showResult(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // 🔔 Start first notification service
    private fun startNotificationService(id: String) {
        NotificationService.trackingCompletion.observe(this, Observer {
            showResult("NotificationService completed for ID $id")
        })
        val serviceIntent = Intent(this, NotificationService::class.java).apply {
            putExtra(NotificationService.EXTRA_ID, id)
        }
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    // 🔔 Start second notification service after third worker
    private fun startSecondNotificationService(id: String) {
        SecondNotificationService.trackingCompletion.observe(this, Observer {
            showResult("SecondNotificationService completed for ID $id")
        })
        val intent = Intent(this, SecondNotificationService::class.java).apply {
            putExtra(SecondNotificationService.EXTRA_ID, id)
        }
        ContextCompat.startForegroundService(this, intent)
    }
}
