package com.example.myapplication

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemArtigoBinding // Corrigido o caminho do binding

class ArtigoAdapter(
    private val onItemClick: (Artigo) -> Unit,
    private val onItemLongClick: (Artigo) -> Unit
) : ListAdapter<Artigo, ArtigoAdapter.ArtigoViewHolder>(ArtigoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtigoViewHolder {
        val binding = ItemArtigoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ArtigoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ArtigoViewHolder, position: Int) {
        val artigo = getItem(position)
        holder.bind(artigo)
    }

    inner class ArtigoViewHolder(private val binding: ItemArtigoBinding) : // Corrigido: `ItemArtigoBinding`
        RecyclerView.ViewHolder(binding.root) {
        fun bind(artigo: Artigo) {
            binding.textArtigoNome.text = artigo.nome // Corrigido: `binding.textArtigoNome`
            binding.textArtigoPreco.text = "R$ ${String.format("%.2f", artigo.precoUnitario)}" // Corrigido: `binding.textArtigoPreco`, usando precoUnitario
            binding.textArtigoDescricao.text = artigo.descricao // Corrigido: `binding.textArtigoDescricao`
            binding.textArtigoNumeroSerie.text = artigo.numeroSerie // Corrigido: `binding.textArtigoNumeroSerie`, usando numeroSerie

            binding.root.setOnClickListener {
                onItemClick(artigo)
            }
            binding.root.setOnLongClickListener {
                onItemLongClick(artigo)
                true
            }
        }
    }

    class ArtigoDiffCallback : DiffUtil.ItemCallback<Artigo>() {
        override fun areItemsTheSame(oldItem: Artigo, newItem: Artigo): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Artigo, newItem: Artigo): Boolean {
            return oldItem == newItem
        }
    }
}