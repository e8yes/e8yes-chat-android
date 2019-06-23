package io.keyu.chat.ui.dashboard

import android.os.AsyncTask
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import kotlinx.android.synthetic.main.fragment_dashboard.*
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
        dashboardViewModel.text.observe(this, Observer {
            textView.text = it
        })

        getVersionButton?.setOnClickListener {
            GrpcTask(activity)
                .execute(
                    "localhost",
                    "",
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
                channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
                val stub = SystemServiceGrpc.newBlockingStub(channel)
                val request = Empty.newBuilder().build()
                val reply = stub.version(request)
                reply.version
            } catch (e: Exception) {
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                e.printStackTrace(pw)
                pw.flush()

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