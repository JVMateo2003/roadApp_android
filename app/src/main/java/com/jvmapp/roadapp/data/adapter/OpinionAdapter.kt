package com.jvmapp.roadapp.data.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jvmapp.roadapp.R
import com.jvmapp.roadapp.data.model.Opinion

class OpinionAdapter(
    private val opiniones: List<Opinion>,
    private val onClick: (Opinion) -> Unit

) : RecyclerView.Adapter<OpinionAdapter.OpinionViewHolder>() {

    inner class OpinionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUsuario: TextView = itemView.findViewById(R.id.tvUsuario)
        val ratingBar: RatingBar = itemView.findViewById(R.id.ratingBar)
        val tvComentario: TextView = itemView.findViewById(R.id.tvComentario)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OpinionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_opinion, parent, false)
        return OpinionViewHolder(view)
    }

    override fun onBindViewHolder(holder: OpinionViewHolder, position: Int) {
        val opinion = opiniones[position]

        holder.itemView.setOnClickListener {
            onClick(opinion)
        }


        holder.tvUsuario.text = opinion.usuario
        holder.ratingBar.rating = opinion.estrellas.toFloat()
        holder.tvComentario.text = opinion.comentario
    }

    override fun getItemCount(): Int = opiniones.size
}