package com.example.myapplication

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemClienteBloqueadoBinding

class ClienteBloqueadoAdapter(
    private val onItemClick: (Cliente) -> Unit,
    private val onItemLongClick: (Cliente) -> Unit
) : ListAdapter<Cliente, ClienteBloqueadoAdapter.ClienteBloqueadoViewHolder>(ClienteBloqueadoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClienteBloqueadoViewHolder {
        val binding = ItemClienteBloqueadoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ClienteBloqueadoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ClienteBloqueadoViewHolder, position: Int) {
        val cliente = getItem(position)
        holder.bind(cliente)
    }

    inner class ClienteBloqueadoViewHolder(private val binding: ItemClienteBloqueadoBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(cliente: Cliente) {
            binding.textNomeClienteBloqueado.text = cliente.nome
            binding.textTelefoneBloqueado.text = cliente.telefone
            binding.textEmailBloqueado.text = cliente.email

            binding.buttonDesbloquear.setOnClickListener {
                onItemClick(cliente)
            }
            binding.buttonDesbloquear.setOnLongClickListener {
                onItemLongClick(cliente)
                true
            }
        }
    }

    class ClienteBloqueadoDiffCallback : DiffUtil.ItemCallback<Cliente>() {
        override fun areItemsTheSame(oldItem: Cliente, newItem: Cliente): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsAreSame(oldItem: Cliente, newItem: Cliente): Boolean {
            return oldItem == newItem
        }
    }
}