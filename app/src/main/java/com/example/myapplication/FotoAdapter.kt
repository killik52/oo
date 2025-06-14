package com.example.myapplication

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.databinding.ItemFotoBinding // Caminho do binding correto

class FotoAdapter(
    private val fotos: MutableList<Uri>,
    private val onFotoClick: (Uri) -> Unit,
    private val onFotoLongClick: (Uri) -> Unit
) : RecyclerView.Adapter<FotoAdapter.FotoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FotoViewHolder {
        val binding = ItemFotoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FotoViewHolder, position: Int) {
        val fotoUri = fotos[position]
        holder.bind(fotoUri)
    }

    override fun getItemCount(): Int = fotos.size

    fun adicionarFoto(uri: Uri) {
        fotos.add(uri)
        notifyItemInserted(fotos.size - 1)
    }

    fun removerFoto(uri: Uri) {
        val position = fotos.indexOf(uri)
        if (position != -1) {
            fotos.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun getFotos(): List<Uri> = fotos

    inner class FotoViewHolder(private val binding: ItemFotoBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(fotoUri: Uri) {
            // Usar Glide para carregar a imagem eficientemente
            Glide.with(binding.imageView.context) // Corrigido: `binding.imageView`
                .load(fotoUri)
                .centerCrop()
                .into(binding.imageView) // Corrigido: `binding.imageView`

            binding.imageView.setOnClickListener { // Corrigido: `binding.imageView`
                onFotoClick(fotoUri)
            }
            binding.imageView.setOnLongClickListener { // Corrigido: `binding.imageView`
                onFotoLongClick(fotoUri)
                true
            }
        }
    }
}