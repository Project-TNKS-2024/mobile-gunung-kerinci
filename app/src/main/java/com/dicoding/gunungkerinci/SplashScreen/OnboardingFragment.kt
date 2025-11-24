package com.dicoding.gunungkerinci.SplashScreen

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.dicoding.gunungkerinci.R

class OnboardingFragment : Fragment() {

    companion object{
        private const val ARG_TITLE = "title"
        private const val ARG_DESC = "desc"

        fun newInstance(title: String, desc: String): OnboardingFragment {
            val fragment = OnboardingFragment()
            val args = Bundle()
            args.putString(ARG_TITLE, title)
            args.putString(ARG_DESC, desc)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_onboarding, container, false)

        val titleText: TextView = view.findViewById(R.id.textBesar)
        val descText: TextView = view.findViewById(R.id.textDeskripsi)

        arguments?.let {
            titleText.text = it.getString(ARG_TITLE)
            descText.text = it.getString(ARG_DESC)
        }

        return view
    }
}