package io.keyu.chat.ui.dashboard

import android.os.AsyncTask
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.keyu.chat.Empty
import io.keyu.chat.R
import io.keyu.chat.SystemServiceGrpc
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

class DashboardFragment : Fragment() {

    private lateinit var dashboardViewModel: DashboardViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dashboardViewModel =
            ViewModelProviders.of(this).get(DashboardViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_dashboard, container, false)
        val textView: TextView = root.findViewById(R.id.dashboardText)
        val s: Button = root.findViewById(R.id.getVersionButton)
        dashboardViewModel.text.observe(this, Observer {
            textView.text = it
        })

        s.setOnClickListener {
            GrpcTask(activity)
                .execute(
                    "10.0.2.2",
                    "haha",
                    "50051"
                )
        }

        return root
    }


    private class GrpcTask(fragmentActivity: FragmentActivity?) : AsyncTask<String, Void, String>() {

        private val activityReference: WeakReference<FragmentActivity?> = WeakReference(fragmentActivity)
        private var channel: ManagedChannel? = null

        override fun doInBackground(vararg params: String): String {
            val host = params[0]
            val portStr = params[2]
            val port = if (TextUtils.isEmpty(portStr)) 0 else Integer.valueOf(portStr)
            return try {
                channel = ManagedChannelBuilder.forAddress("10.0.2.2", 50051).usePlaintext().build()
                val stub = SystemServiceGrpc.newBlockingStub(channel)
                val request = Empty.newBuilder().build()
                val reply = stub.version(request)
                Log.v("hehe", reply.version)
                reply.version
            } catch (e: Exception) {
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                e.printStackTrace(pw)
                pw.flush()

                Log.e("hehe", "Failed to fetch version : %s".format(sw))

                "Failed to fetch version : %s".format(sw)
            }
        }

        override fun onPostExecute(result: String) {
            try {
                channel?.shutdown()?.awaitTermination(1, TimeUnit.SECONDS)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }

            val activity = activityReference.get() ?: return
            val dashboardText: TextView = activity.findViewById(R.id.dashboardText)

            dashboardText.text = result
        }
    }
}
