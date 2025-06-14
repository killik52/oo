package com.example.myapplication

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemFaturaLixeiraBinding // Supondo que este é o layout da lixeira
import java.text.NumberFormat
import java.util.Locale

class FaturaResumidaAdapter( // Renomeado de FaturaLixeiraAdapter
    private val onItemClick: (Long) -> Unit,
    private val onItemLongClick: (Long) -> Unit
) : ListAdapter<FaturaResumidaItem, FaturaResumidaAdapter.FaturaResumidaViewHolder>(FaturaResumidaDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaturaResumidaViewHolder {
        val binding = ItemFaturaLixeiraBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FaturaResumidaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FaturaResumidaViewHolder, position: Int) {
        val fatura = getItem(position)
        holder.bind(fatura)
    }

    inner class FaturaResumidaViewHolder(private val binding: ItemFaturaLixeiraBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(fatura: FaturaResumidaItem) {
            binding.textNumeroFaturaLixeira.text = "${binding.root.context.getString(R.string.invoice_number_label)} ${Constants.DEFAULT_INVOICE_PREFIX}${fatura.id}"
            binding.textNomeClienteLixeira.text = "${binding.root.context.getString(R.string.client_name_label)} ${fatura.clienteNome}"
            binding.textDataFaturaLixeira.text = "${binding.root.context.getString(R.string.date_label)} ${fatura.data}"
            binding.textTotalFaturaLixeira.text = "${binding.root.context.getString(R.string.total_label)} ${NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(fatura.total)}"

            if (fatura.foiEnviada) {
                binding.textStatusFaturaLixeira.text = binding.root.context.getString(R.string.status_sent)
                binding.textStatusFaturaLixeira.setTextColor(ContextCompat.getColor(binding.root.context, R.color.green_700))
            } else {
                binding.textStatusFaturaLixeira.text = binding.root.context.getString(R.string.status_pending)
                binding.textStatusFaturaLixeira.setTextColor(ContextCompat.getColor(binding.root.context, R.color.red_700))
            }

            // A FaturaResumidaItem pode ter um campo de data de exclusão se estiver na lixeira
            // Se você adicionou dataExclusao na FaturaResumidaItem para a lixeira,
            // você precisaria adicionar uma TextView para exibi-la e uma propriedade no FaturaResumidaItem.
            // Por enquanto, o FaturaResumidaItem não tem essa propriedade diretamente.

            binding.root.setOnClickListener {
                onItemClick(fatura.id)
            }
            binding.root.setOnLongClickListener {
                onItemLongClick(fatura.id)
                true
            }
        }
    }

    class FaturaResumidaDiffCallback : DiffUtil.ItemCallback<FaturaResumidaItem>() {
        override fun areItemsTheSame(oldItem: FaturaResumidaItem, newItem: FaturaResumidaItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FaturaResumidaItem, newItem: FaturaResumidaItem): Boolean {
            return oldItem == newItem
        }
    }
}